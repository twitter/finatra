namespace java com.twitter.snakeCase.thriftjava
#@namespace scala com.twitter.snakeCase.thriftscala
namespace rb SnakeCaseService

struct NotificationEvent {
  1: required bool high_priority = false
  2: required string source_event_name
}

struct ConversationEvent {
  1: required i64 conversation_id
  2: required i64 tweet_id
  3: required i64 user_id
  4: required list<i64> subscribed_user_ids
  5: required i64 event_time_ms
}

union EnqueableEvents {
  1: NotificationEvent notificationEvent
  2: ConversationEvent conversationEvent
}

struct EnqueueEventRequest {
  1: required EnqueableEvents event
}

service SnakeCaseService {
    bool enqueue_event(1: EnqueueEventRequest request)

    bool dequeue_event(1: EnqueueEventRequest request)
}

service ExtendedSnakeCaseService extends SnakeCaseService {
    bool additional_event(1: EnqueueEventRequest request)
}
