package org.gridkit.zerormi.io;

import java.io.IOException;

import org.junit.Test;

public class BytePipeBasicSourceSideTest extends ByteSourceBasicChecks {

	BytePipeTestSupport support = new BytePipeTestSupport(256);
	
	@Override
	protected ByteSourceTestSupport getSourceTestSuppport() {
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
		super.write_whole_read_by_x("Not so long message", 1, false);
	}

	@Test
	public void small_message_whole_by_2() throws IOException {
		super.write_whole_read_by_x("Not so long message", 2, false);
	}

	@Test
	public void small_message_whole_by_3() throws IOException {
		super.write_whole_read_by_x("Not so long message", 3, false);
	}

	@Test
	public void small_message_whole_by_4() throws IOException {
		super.write_whole_read_by_x("Not so long message", 4, false);
	}

	@Test
	public void small_message_whole_by_5() throws IOException {
		super.write_whole_read_by_x("Not so long message", 5, false);
	}
	
	@Test
	public void async_source_termination_with_no_data_and_fast_close() throws IOException, InterruptedException {
		super.async_source_termination("", 256, false, true, false);
	}

	@Test
	public void async_source_termination_with_no_data_and_slow_close() throws IOException, InterruptedException {
		super.async_source_termination("", 256, false, false, true);
	}

	@Test
	public void async_source_termination_with_small_buffer() throws IOException, InterruptedException {
		super.async_source_termination("Not so long message", 4, false, false, false);
	}

	@Test
	public void async_source_termination_with_small_buffer_and_fast_close() throws IOException, InterruptedException {
		super.async_source_termination("Not so long message", 4, false, true, false);
	}

	@Test
	public void async_source_termination_with_small_buffer_and_slow_close() throws IOException, InterruptedException {
		super.async_source_termination("Not so long message", 4, false, false, true);
	}

	@Test
	public void async_source_termination_with_large_buffer() throws IOException, InterruptedException {
		super.async_source_termination("Not so long message", 256, false, false, false);
	}

	@Test
	public void async_source_termination_with_large_buffer_and_fast_close() throws IOException, InterruptedException {
		super.async_source_termination("Not so long message", 256, false, true, false);
	}

	@Test
	public void async_source_termination_with_large_buffer_and_slow_close() throws IOException, InterruptedException {
		super.async_source_termination("Not so long message", 256, false, false, true);
	}
	
	@Test
	@Override
	public void available_state_after_eof() throws IOException {
		super.available_state_after_eof();
	}

	@Test
	@Override
	public void available_state_after_reader_close() throws IOException {
		super.available_state_after_reader_close();
	}

	@Test
	public void data_stream_until_eof_10k_by_1000_heap() throws IOException {
		super.data_stream_until_eof(10 << 10, 10000, false);
	}

	@Test
	public void data_stream_until_eof_10k_by_13_heap() throws IOException {
		super.data_stream_until_eof(10 << 10, 13, false);
	}

	@Test
	public void data_stream_until_eof_100k_by_4k_heap() throws IOException {
		super.data_stream_until_eof(100 << 10, 4 << 10, false);
	}

	@Test
	public void data_stream_until_eof_100k_by_4k_direct() throws IOException {
		super.data_stream_until_eof(100 << 10, 4 << 10, true);
	}	

	@Test
	public void data_stream_while_available_10k_by_1000_heap() throws IOException {
		super.data_stream_while_available(10 << 10, 10000, false);
	}
	
	@Test
	public void data_stream_while_available_10k_by_13_heap() throws IOException {
		super.data_stream_while_available(10 << 10, 13, false);
	}
	
	@Test
	public void data_stream_while_available_100k_by_4k_heap() throws IOException {
		super.data_stream_while_available(100 << 10, 4 << 10, false);
	}
	
	@Test
	public void data_stream_while_available_100k_by_4k_direct() throws IOException {
		super.data_stream_while_available(100 << 10, 4 << 10, true);
	}	
	
	
}
