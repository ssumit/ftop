package co.riva.door;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

//@NotThreadSafe
class FrameMaker
{
    @Nullable private FrameListener _listener;
    static int BUFFERSIZE = 65355;
    static int HEADERSIZE = 4;
    @NotNull private byte[] _buffer = new byte[BUFFERSIZE];
    private int _position = -1;

    public interface FrameListener
    {
        void onNewFrame(byte[] buffer);
    }

    public void consume(@NotNull ByteBuffer newBuffer)
    {
        newBuffer.rewind();
        merge(_buffer, newBuffer);
        int frameSize = getCurrentFrameSize(_buffer);
        while (frameSize <= _position + 1 - HEADERSIZE)
        {
            //frame it and adjust position
            int to = frameSize + HEADERSIZE;
            fireFrame(Arrays.copyOfRange(_buffer, HEADERSIZE, to));
            compact(frameSize);
            if (_position > 4)
            {
                frameSize = getCurrentFrameSize(_buffer);
            }
            else
            {
                return;
            }
        }
    }

    private void compact(int frameSize)
    {
        int start = HEADERSIZE + frameSize;
        if (start <= _position)
        {
            byte[] result = Arrays.copyOfRange(_buffer, start, _position + 1);
            _position = result.length - 1;
            _buffer = result;
        }
        else
        {
            _buffer = new byte[BUFFERSIZE];
            _position = -1;
        }
    }

    private void merge(byte[] oldData, ByteBuffer newData)
    {
        if (_position < 0)
        {
            _buffer = newData.array();
            _position = newData.remaining() - 1;
        }
        else
        {
            int newDataSize = newData.remaining();
            int mergedSize = newDataSize + _position + 1;
            if (mergedSize > _buffer.length)
            {
                byte[] biggerBuffer = new byte[mergedSize];
                System.arraycopy(oldData, 0, biggerBuffer, 0, _position + 1);
                System.arraycopy(newData.array(), 0, biggerBuffer, _position + 1, newDataSize);
                _position = mergedSize - 1;
                _buffer = biggerBuffer;
            }
            else
            {
                System.arraycopy(newData.array(), 0, oldData, _position + 1, newDataSize);
                _position = newDataSize + _position;
            }
        }
    }

    public static byte[] frame(@NotNull byte[] payload)
    {
        int capacity = payload.length + 4;
        ByteBuffer b = ByteBuffer.allocate(capacity);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putInt(payload.length);
        b.put(payload, 0, payload.length);

        byte[] result = new byte[capacity];
        b.rewind();
        b.get(result);
        return result;
    }

    public void addListener(@Nullable FrameListener listener)
    {
        _listener = listener;
    }

    /*privates*/

    private void fireFrame(byte[] result)
    {
        if (_listener != null)
        {
            _listener.onNewFrame(result);
        }
    }

    private int getCurrentFrameSize(@NotNull byte[] content)
    {
        ByteBuffer temp = ByteBuffer.wrap(content);
        temp.order(ByteOrder.BIG_ENDIAN);
        int anInt = temp.getInt();
        return anInt;
    }
}
