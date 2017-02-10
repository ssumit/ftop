package co.riva.door;

import java.nio.ByteBuffer;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

class AppendableBuffer {
    private ByteBuffer buffer;

    public ByteBuffer append(ByteBuffer data) {
        ByteBuffer nb = ByteBuffer.allocate(calculateSize(data));
        if (notNull()) {
            nb.put(buffer);
            clear();
        }
        nb.put(data);
        return nb;
    }

    public void set(ByteBuffer data) {
        checkNotNull(data);
        if (data.hasRemaining()) {
            buffer = ByteBuffer.allocate(data.remaining());
            buffer.put(data);
            buffer.rewind();
        }
    }

    public void clear() {
        buffer = null;
    }

    public boolean hasRemaining() {
        if (notNull()) {
            return buffer.hasRemaining();
        }
        return false;
    }

    public Optional<ByteBuffer> get() {
        return Optional.ofNullable(buffer);
    }
    /* private */

    private int calculateSize(ByteBuffer data) {
        int result = data.limit();
        if (notNull()) {
            result += buffer.remaining();
        }
        return result;
    }

    private boolean notNull() {
        return buffer != null;
    }
}