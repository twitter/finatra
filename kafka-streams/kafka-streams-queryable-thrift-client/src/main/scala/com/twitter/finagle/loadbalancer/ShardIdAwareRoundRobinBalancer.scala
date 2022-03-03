package com.twitter.finagle.loadbalancer

import com.twitter.finagle.Address.Inet
import com.twitter.finagle._
import com.twitter.finagle.context.Contexts
import com.twitter.finagle.loadbalancer.LoadBalancerFactory.PanicMode
import com.twitter.finagle.partitioning.zk.ZkMetadata
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.streams.queryable.thrift.domain.RequestedShardIds
import com.twitter.finatra.streams.queryable.thrift.domain.ServiceShardId
import com.twitter.util.Activity
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import com.twitter.util.Time
import scala.collection.mutable

//TODO: DRY with com.twitter.finagle.loadbalancer.Balancers
object ShardIdAwareRoundRobinBalancer {
  def create: LoadBalancerFactory = new LoadBalancerFactory {
    override def newBalancer[Req, Rep](
      endpoints: Activity[IndexedSeq[EndpointFactory[Req, Rep]]],
      exc: NoBrokersAvailableException,
      params: Stack.Params
    ): ServiceFactory[Req, Rep] = {
      val sr = params[param.Stats].statsReceiver
      val balancer = new ShardIdAwareRoundRobinBalancer(endpoints, sr, exc)
      newScopedBal(params[param.Label].label, sr, "round_robin", balancer)
    }

    override def toString: String = "ShardAwareRoundRobin"
  }

  private def newScopedBal[Req, Rep](
    label: String,
    sr: StatsReceiver,
    lbType: String,
    bal: ServiceFactory[Req, Rep]
  ): ServiceFactory[Req, Rep] = {
    bal match {
      case balancer: Balancer[Req, Rep] => balancer.register(label)
      case _ => ()
    }

    new ServiceFactoryProxy(bal) {
      private[this] val typeGauge = sr.scope("algorithm").addGauge(lbType)(1)

      override def close(when: Time): Future[Unit] = {
        typeGauge.remove()
        super.close(when)
      }
    }
  }
}

/**
 * A shard id aware balancer that lets clients specify a list of requested shard ids through a Local
 *
 * @see com.twitter.finatra.streams.queryable.thrift.client.internal.RequestedShardIds
 *
 * This class is adapted from Finagle RoundRobinLoadBalancer
 * TODO: DRY with RoundRobinLoadBalancer
 *
 * TODO: This load balancer currently only works with zookeeper-backed destinations.
 * A general solution could extract a ShardIdentifier interface e.g.
 * trait ShardIdentifier {
 *   def fromAddrMetadata(meta: Addr.Metadata): Option[Int]
 * }
 *
 * which we could then implement when implementing a new Announcer/Resolver pair ie.
 *
 * class ZkShardIdentifier extends ShardIdentifier {
 *   def fromAddrMetadata(meta: Addr.Metadata): Option[Int] = ZkMetadata.fromAddrMetadata(meta).shardId
 * }
 */
private final class ShardIdAwareRoundRobinBalancer[Req, Rep](
  protected val endpoints: Activity[IndexedSeq[EndpointFactory[Req, Rep]]],
  protected val statsReceiver: StatsReceiver,
  protected val emptyException: NoBrokersAvailableException,
  private[loadbalancer] val panicMode: PanicMode = PanicMode.MajorityUnhealthy)
    extends ServiceFactory[Req, Rep]
    with Balancer[Req, Rep]
    with Updating[Req, Rep]
    with Logging {

  override def additionalMetadata: Map[String, Any] = Map.empty

  override def initDistributor(): Distributor = new Distributor(Vector.empty)

  override def newNode(factory: EndpointFactory[Req, Rep]): Node = new Node(factory)

  protected class Node(val factory: EndpointFactory[Req, Rep])
      extends ServiceFactoryProxy[Req, Rep](factory)
      with NodeT[Req, Rep] {

    // Note: These stats are never updated.
    override def load: Double = 0.0

    override def pending: Int = 0

    override def close(deadline: Time): Future[Unit] = {
      factory.close(deadline)
    }

    override def apply(conn: ClientConnection): Future[Service[Req, Rep]] = {
      factory(conn)
    }

    //Updated from RoundRobinBalancer
    def shardId: Option[Int] = {
      val shardId = factory.address match {
        case inet: Inet =>
          for {
            zkMetadata <- ZkMetadata.fromAddrMetadata(inet.metadata)
            shardId <- zkMetadata.shardId
          } yield {
            shardId
          }
        case _ =>
          None
      }

      if (shardId.isEmpty) {
        //TODO: Should this be fatal?
        error(
          s"ShardIdAwareRoundRobinBalancer should only be used with nodes containing a shardId. No shardId found for $this"
        )
      }

      shardId
    }
  }

  /**
   * A simple round robin distributor.
   */
  protected class Distributor(vector: Vector[Node]) extends DistributorT[Node](vector) {
    type This = Distributor

    /**
     * Indicates if we've seen any down nodes during `pick` which we expected to be available
     */
    @volatile
    protected[this] var sawDown = false

    /**
     * `up` is the Vector of nodes that were `Status.Open` at creation time.
     * `down` is the Vector of nodes that were not `Status.Open` at creation time.
     */
    private[this] val (up: Vector[Node], down: Vector[Node]) = vector.partition(_.isAvailable)

    //Updated for ShardAware balancing
    private val shardIdToNode = new mutable.LongMap[Node](vector.size)

    /* Populate shardIdToNode map at construction time */
    for {
      node <- vector
      shardId <- node.shardId
      prevNode <- shardIdToNode.put(shardId.toLong, node)
    } {
      warn(
        s"Multiple nodes assigned the same shardId! prevNode: $prevNode newNode: $node"
      ) //TODO: Zombie detection?
    }

    private val availableShardIds = shardIdToNode.keys.map(id => ServiceShardId(id.toInt)).toSet

    private val noBrokersAvailableNode = failingNode(new NoBrokersAvailableException)
    private val noRequestedBrokersAvailableNode = failingNode(
      new NoRequestedBrokersAvailableException
    )
    private val noRequestedShardIdsNode = failingNode(new NoRequestedShardIdsException)

    override def pick(): Node = {
      if (vector.isEmpty) {
        return noBrokersAvailableNode
      }

      Contexts.local.get(RequestedShardIds.requestedShardIdsKey) match {
        case Some(requestedShardIds) =>
          requestedShardIds.chooseShardId(availableShardIds) match {
            case Some(chosenShardId) =>
              val node =
                shardIdToNode(
                  chosenShardId.id.toLong
                ) //Note: chooseShardId ensures that we choose a shardId that exists in the map
              if (node.status != Status.Open) {
                sawDown = true
              }
              debug(s"PICK with requested shards $requestedShardIds selects node $node")
              node
            case None =>
              noRequestedBrokersAvailableNode
          }
        case _ => //
          noRequestedShardIdsNode
      }
    }

    override def rebuild(): This = new Distributor(vector)

    override def rebuild(vector: Vector[Node]): This = new Distributor(vector)

    // while the `nonEmpty` check isn't necessary, it is an optimization
    // to avoid the iterator allocation in the common case where `down`
    // is empty.
    override def needsRebuild: Boolean = {
      sawDown || (down.nonEmpty && down.exists(_.isAvailable))
    }
  }
}
