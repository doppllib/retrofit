import co.touchlab.doppl.testing.DopplJunitTestHelper;

/**
 * Created by kgalligan on 8/20/17.
 */

public class RunAll
{
    public static void runAll()
    {
        new Thread()
        {
            @Override
            public void run()
            {
//                DopplJunitTestHelper.runResource("dopplTests.txt");
//                DopplJunitTestHelper.runSpecific("retrofit2.RetrofitTest#callbackExecutorUsedForFailure");
                DopplJunitTestHelper.runSpecific("retrofit2.CallTest");
            }
        }.start();
    }

    public static void runOne(final String s)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                DopplJunitTestHelper.runSpecific(s);
            }
        }.start();
    }
}
