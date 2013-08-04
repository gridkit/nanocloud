package org.gridkit.zerormi.io;

import java.io.IOException;

import org.junit.Test;

public class BytePipeBasicSinkSideTest extends ByteSinkBasicChecks {

	ByteSinkTestSupport support = new BytePipeTestSupport(256);
	
	@Override
	protected ByteSinkTestSupport getSinkTestSuppport() {
		return support;
	}

	@Test
	@Override
	public void small_message_whole() throws IOException {
		super.small_message_whole();
	}

	@Test
	@Override
	public void small_message_whole_with_direct_buffer() throws IOException {
		super.small_message_whole_with_direct_buffer();
	}

	@Test
	public void small_message_whole_by_1() throws IOException {
		super.small_message_whole_by_x(1);
	}

	@Test
	public void small_message_whole_by_2() throws IOException {
		super.small_message_whole_by_x(2);
	}

	@Test
	public void small_message_whole_by_3() throws IOException {
		super.small_message_whole_by_x(3);
	}

	@Test
	public void small_message_whole_by_4() throws IOException {
		super.small_message_whole_by_x(4);
	}

	@Test
	public void small_message_whole_by_5() throws IOException {
		super.small_message_whole_by_x(5);
	}
	
	@Test
	public void async_sink_termination() throws IOException {
		// TODO test is falling sporadically
		super.async_sink_termination();
	}

	@Test
	public void data_stream_10k_by_1000_heap() throws IOException {
		super.data_stream(10 << 10, 10000, false);
	}

	@Test
	public void data_stream_10k_by_13_heap() throws IOException {
		super.data_stream(10 << 10, 13, false);
	}

	@Test
	public void data_stream_100k_by_4k_heap() throws IOException {
		super.data_stream(100 << 10, 4 << 10, false);
	}

	@Test
	public void data_stream_100k_by_4k_direct() throws IOException {
		super.data_stream(100 << 10, 4 << 10, true);
	}	
	
	
}
