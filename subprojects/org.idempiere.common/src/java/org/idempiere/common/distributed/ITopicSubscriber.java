package org.idempiere.common.distributed;

public interface ITopicSubscriber<T> {
	public void onMessage(T message);
}
