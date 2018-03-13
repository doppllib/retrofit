package retrofit2.clientfactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;


import static org.junit.Assert.*;

/**
 * Created by kgalligan on 12/18/17.
 */
public class CallClientFactoryTest
{
    @Before
    public void setUp() throws Exception
    {
    }

    @Test
    public void initIosTest()
    {
        CallClientFactory callClientFactory = new CallClientFactory();

        CallClientFactory.UrlSessionBuilder sessionBuilder = Mockito.mock(CallClientFactory.UrlSessionBuilder.class);
        callClientFactory.initIos(sessionBuilder);

//        Mockito.verify(sessionBuilder, )

    }

}