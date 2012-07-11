// package com.twitter.finatra

// import org.jboss.netty.handler.codec.http._
// import org.jboss.netty.handler.codec.http.{Cookie => JCookie}
// import scala.collection.JavaConversions._


// object CookieAdapter {

//   def portsOf(c: JCookie): Cookie = {
//     var ints = Set[Int]()
//     c.getPorts.foreach { xs =>
//       ints += xs
//     }
//     ints
//   }

//   def in(c: JCookie): FinatraCookie = {
//     new FinatraCookie(
//       expires=c.getMaxAge,
//       comment=c.getComment,
//       commentUrl=c.getCommentUrl,
//       domain=c.getDomain,
//       path=c.getPath,
//       ports=portsOf(c),
//       name=c.getName,
//       version=c.getVersion,
//       isDiscard=c.isDiscard,
//       isHttpOnly=c.isHttpOnly,
//       isSecure=c.isSecure,
//       value=c.getValue
//     )
//   }

//   def out(cookies: Map[String, FinatraCookie]): String = {
//     val cookieEncoder = new CookieEncoder(false)
//     cookies.foreach { cookie =>
//       val ck = new DefaultCookie(cookie._1, cookie._2.value)
//       ck.setComment(cookie._2.comment)
//       ck.setMaxAge(cookie._2.expires)
//       ck.setCommentUrl(cookie._2.commentUrl)
//       ck.setDiscard(cookie._2.isDiscard)
//       ck.setDomain(cookie._2.domain)
//       ck.setHttpOnly(cookie._2.isHttpOnly)
//       ck.setPath(cookie._2.path)
//       ck.setSecure(cookie._2.isSecure)
//       ck.setValue(cookie._2.value)
//       ck.setVersion(cookie._2.version)
//       cookieEncoder.addCookie(ck)
//     }
//     cookieEncoder.encode
//   }
// }
