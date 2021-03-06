package com.disruptor;

import clojure.main;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.SingleThreadedClaimStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;

public class DisruptorHelper {
	/**
	 * ringbuffer容量，最好是2的N次方
	 */
	private static final int BUFFER_SIZE = 1024 * 8;
	private RingBuffer<DeliveryReportEvent> ringBuffer;//环形的缓冲区
	private SequenceBarrier sequenceBarrier;
	private DeliveryReportEventHandler handler;
	private BatchEventProcessor<DeliveryReportEvent> batchEventProcessor;
	private static DisruptorHelper instance;
	private static boolean inited = false;

	private DisruptorHelper() {
		ringBuffer = new RingBuffer<DeliveryReportEvent>(DeliveryReportEvent.EVENT_FACTORY, new SingleThreadedClaimStrategy(BUFFER_SIZE), new YieldingWaitStrategy());
		sequenceBarrier = ringBuffer.newBarrier();
		handler = new DeliveryReportEventHandler();
		batchEventProcessor = new BatchEventProcessor<DeliveryReportEvent>(ringBuffer, sequenceBarrier, handler);
		ringBuffer.setGatingSequences(batchEventProcessor.getSequence());
	}

	public static void initAndStart() {
		instance = new DisruptorHelper();
		new Thread(instance.batchEventProcessor).start();
		inited = true;
	}

	public static void shutdown() {
		if (!inited) {
			throw new RuntimeException("Disruptor还没有初始化！");
		}
		instance.shutdown0();
	}

	private void shutdown0() {
		batchEventProcessor.halt();
	}

	private void produce0(DeliveryReport deliveryReport) {
		// 获取下一个序列号
		long sequence = ringBuffer.next();
		// 将状态报告存入ringBuffer的该序列号中
		ringBuffer.get(sequence).setDeliveryReport(deliveryReport);
		// 通知消费者该资源可以消费
		ringBuffer.publish(sequence);
	}

	/**
	 * 将状态报告放入资源队列，等待处理
	 * 
	 * @param deliveryReport
	 */
	public static void produce(DeliveryReport deliveryReport) {
		if (!inited) {
			throw new RuntimeException("Disruptor还没有初始化！");
		}
		instance.produce0(deliveryReport);
	}
	public static void main(String[] args) {
		 DeliveryReport deliveryReport = new DeliveryReport();
		 deliveryReport.setTest("1111");
		 DisruptorHelper.initAndStart();
		 DisruptorHelper.produce(deliveryReport);
	}
}