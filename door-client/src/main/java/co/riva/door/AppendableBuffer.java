package co.riva.door;

import java.nio.ByteBuffer;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

class AppendableBuffer {
    private ByteBuffer _;

    public ByteBuffer append(ByteBuffer data) {
        ByteBuffer nb = ByteBuffer.allocate(calculateSize(data));
        if (notNull()) {
            nb.put(_);
            clear();
        }
        nb.put(data);
        return nb;
    }

    public void set(ByteBuffer data) {
        checkNotNull(data);
        if (data.hasRemaining()) {
            _ = ByteBuffer.allocate(data.remaining());
            _.put(data);
            _.rewind();
        }
    }

    public void clear() {
        _ = null;
    }

    public boolean hasRemaining() {
        if (notNull()) {
            return _.hasRemaining();
        }
        return false;
    }

    public Optional<ByteBuffer> get() {
        return Optional.ofNullable(_);
    }
    /* private */

    private int calculateSize(ByteBuffer data) {
        int result = data.limit();
        if (notNull()) {
            result += _.remaining();
        }
        return result;
    }

    private boolean notNull() {
        return _ != null;
    }
}