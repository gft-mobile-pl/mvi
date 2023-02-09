package com.gft.data

class ConsumableEvent<T> (private val payload: T) {
    /**
     * States whether the event has been consumed already.
     */
    var isConsumed = false
        private set

    /**
     * Consumes the event if it has not been consumed yet.
     * @param consumer  Event consumer that will be invoked if the event has not been consumed yet.
     * @return          Boolean value of 'true' if the event has been provided to the consumer.
     *                  Boolean value of 'false' if the event has been consumed already and it was not provided to the consumer.
     */
    @Synchronized
    fun consume(consumer: (T) -> Unit): Boolean {
        if (isConsumed) return false
        isConsumed = true
        consumer(payload)
        return true
    }

    /**
     * Passes the event to the provided handler if it has not been consumed yet. Handler may or may not consume the event.
     * @param handler   Event handler that will be invoked if the event has not been consumed yet.
     *                  The provided handler must return [true] if it consumes the event or [false] otherwise.
     * @return          Boolean value of 'true' if the event has been provided to the handler and the handler has consumed it (that is - it has been "handled").
     *                  Boolean value of 'false' if the event has been consumed already or if the handler has not consumed the event.
     */
    @Synchronized
    fun consumeOptionally(handler: (T) -> Boolean): Boolean {
        if (isConsumed) return false
        isConsumed = handler(payload)
        return isConsumed
    }

    /**
     * Returns the payload of the event without consuming it.
     */
    fun peekPayload(): T = payload
}
