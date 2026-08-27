package com.autumn.douyin.liquidglass.root;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import static org.junit.Assert.*;

public class CompositeFrameProtocolTest {

    // 语义化测试常量
    private static final int TEST_MAGIC = CompositeFrameProtocol.Magic;
    private static final int TEST_LEFT = 10;
    private static final int TEST_TOP = 20;
    private static final int TEST_RIGHT = 1010;
    private static final int TEST_BOTTOM = 740;
    private static final int TEST_WIDTH = 800;
    private static final int TEST_HEIGHT = 600;
    private static final long TEST_TIMESTAMP = 123456789L;
    private static final String TEST_ERROR_MSG = "capture_failed: surface_destroyed";

    // ==================== 基础解析测试 ====================

    @Test
    public void readsOkHeaderWithTimestamp() throws IOException {
        CompositeFrameHeader header = CompositeFrameProtocol.INSTANCE.read(
                new DataInputStream(buildPacket(CompositeFrameProtocol.StatusOk, TEST_TIMESTAMP, null))
        );

        assertEquals(CompositeFrameProtocol.StatusOk, header.getStatus());
        assertEquals(TEST_TIMESTAMP, header.getFrameTimestamp());
        assertEquals(TEST_LEFT, header.getCaptureLeft());
        assertEquals(TEST_TOP, header.getCaptureTop());
        assertEquals(TEST_RIGHT, header.getCaptureRight());
        assertEquals(TEST_BOTTOM, header.getCaptureBottom());
        assertEquals(TEST_WIDTH, header.getFrameWidth());
        assertEquals(TEST_HEIGHT, header.getFrameHeight());
    }

    @Test
    public void readsIdleHeaderWithTimestamp() throws IOException {
        CompositeFrameHeader header = CompositeFrameProtocol.INSTANCE.read(
                new DataInputStream(buildPacket(CompositeFrameProtocol.StatusIdle, TEST_TIMESTAMP, null))
        );

        assertEquals(CompositeFrameProtocol.StatusIdle, header.getStatus());
        assertEquals(TEST_TIMESTAMP, header.getFrameTimestamp());
    }

    @Test
    public void errorHeaderCorrectlyReadsMessage() throws IOException {
        DataInputStream input = new DataInputStream(
                buildPacket(CompositeFrameProtocol.StatusError, null, TEST_ERROR_MSG)
        );
        CompositeFrameHeader header = CompositeFrameProtocol.INSTANCE.read(input);

        assertEquals(CompositeFrameProtocol.StatusError, header.getStatus());
        assertEquals(TEST_ERROR_MSG, CompositeFrameProtocol.INSTANCE.readError(input));
    }

    @Test
    public void rejectsInvalidStatus() {
        try {
            CompositeFrameProtocol.INSTANCE.read(new DataInputStream(buildPacket(99, null, null)));
            fail("expected invalid status to throw IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    // ==================== 往返一致性测试 ====================

    @Test
    public void writeAndReadOkFrameRoundTrip() throws IOException {
        // 构造原始头
        CompositeFrameHeader original = new CompositeFrameHeader(
                CompositeFrameProtocol.StatusOk,
                TEST_LEFT, TEST_TOP, TEST_RIGHT, TEST_BOTTOM,
                TEST_WIDTH, TEST_HEIGHT,
                TEST_TIMESTAMP
        );

        // 序列化
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bos);
        CompositeFrameProtocol.INSTANCE.write(output, original);
        output.flush();

        // 反序列化
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
        CompositeFrameHeader parsed = CompositeFrameProtocol.INSTANCE.read(input);

        // 全字段校验
        assertEquals(original.getStatus(), parsed.getStatus());
        assertEquals(original.getCaptureLeft(), parsed.getCaptureLeft());
        assertEquals(original.getCaptureTop(), parsed.getCaptureTop());
        assertEquals(original.getCaptureRight(), parsed.getCaptureRight());
        assertEquals(original.getCaptureBottom(), parsed.getCaptureBottom());
        assertEquals(original.getFrameWidth(), parsed.getFrameWidth());
        assertEquals(original.getFrameHeight(), parsed.getFrameHeight());
        assertEquals(original.getFrameTimestamp(), parsed.getFrameTimestamp());
    }

    @Test
    public void writeAndReadErrorFrameRoundTrip() throws IOException {
        String errorMsg = "test_error_message";

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bos);
        CompositeFrameProtocol.INSTANCE.writeError(output, errorMsg);
        output.flush();

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
        CompositeFrameHeader header = CompositeFrameProtocol.INSTANCE.read(input);

        assertEquals(CompositeFrameProtocol.StatusError, header.getStatus());
        assertEquals(errorMsg, CompositeFrameProtocol.INSTANCE.readError(input));
    }

    // ==================== 异常边界测试 ====================

    @Test
    public void rejectsWrongMagic() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bos);
            output.writeInt(0xDEADBEEF); // 错误魔数
            output.writeInt(CompositeFrameProtocol.StatusOk);
            output.flush();

            CompositeFrameProtocol.INSTANCE.read(new DataInputStream(new ByteArrayInputStream(bos.toByteArray())));
            fail("expected wrong magic to throw IOException");
        } catch (IOException expected) {
        }
    }

    @Test
    public void rejectsTruncatedPacket() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bos);
            output.writeInt(TEST_MAGIC);
            output.writeInt(CompositeFrameProtocol.StatusOk);
            output.writeInt(TEST_LEFT); // 只写1个int，不满足头部长度
            output.flush();

            CompositeFrameProtocol.INSTANCE.read(new DataInputStream(new ByteArrayInputStream(bos.toByteArray())));
            fail("expected truncated packet to throw EOFException");
        } catch (EOFException expected) {
        }
    }

    @Test
    public void readsEmptyErrorMessage() throws IOException {
        DataInputStream input = new DataInputStream(
                buildPacket(CompositeFrameProtocol.StatusError, null, "")
        );
        CompositeFrameHeader header = CompositeFrameProtocol.INSTANCE.read(input);

        assertEquals(CompositeFrameProtocol.StatusError, header.getStatus());
        assertEquals("", CompositeFrameProtocol.INSTANCE.readError(input));
    }

    @Test
    public void zeroTimestampIsValid() throws IOException {
        CompositeFrameHeader header = CompositeFrameProtocol.INSTANCE.read(
                new DataInputStream(buildPacket(CompositeFrameProtocol.StatusOk, 0L, null))
        );

        assertEquals(0L, header.getFrameTimestamp());
    }

    // ==================== 工具方法 ====================

    /**
     * 语义化构造测试数据包
     */
    private ByteArrayInputStream buildPacket(
            int status,
            Long timestamp,
            String errorMessage
    ) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);

        output.writeInt(TEST_MAGIC);
        output.writeInt(status);
        // 7个元数据字段：left, top, right, bottom, width, height, 保留位
        output.writeInt(TEST_LEFT);
        output.writeInt(TEST_TOP);
        output.writeInt(TEST_RIGHT);
        output.writeInt(TEST_BOTTOM);
        output.writeInt(TEST_WIDTH);
        output.writeInt(TEST_HEIGHT);
        output.writeInt(0); // 保留字段

        if (timestamp != null) {
            output.writeLong(timestamp);
        }
        if (errorMessage != null) {
            output.writeUTF(errorMessage);
        }

        output.flush();
        return new ByteArrayInputStream(bytes.toByteArray());
    }
}
