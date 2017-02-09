package co.riva.door;


import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class ExceptionThrowingFutureTask extends FutureTask<Object>
{

    public ExceptionThrowingFutureTask(@NotNull Runnable r)
    {
        super(r, null);
    }

    @Override
    protected void done() {
        try
        {
            if (!isCancelled())
            {
                get();
            }
        }
        catch (final ExecutionException e)
        {
            Thread thread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    throw new RuntimeException(e.getMessage(), e);
                }
            });
            thread.start();
        }
        catch (InterruptedException e)
        {
            //ignored
        }
    }
}

