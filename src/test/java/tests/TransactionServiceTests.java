package tests;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import com.n26.atrposki.domain.Transaction;
import com.n26.atrposki.domain.TransactionMadeEventHandler;
import com.n26.atrposki.transactions.TransactionDTO;
import com.n26.atrposki.transactions.TransactionsService;
import com.n26.atrposki.utils.events.TimedEvent;
import com.n26.atrposki.utils.time.ITimeService;
import org.junit.Assert;
import org.junit.Test;
import org.omg.IOP.TransactionService;

import static com.n26.atrposki.utils.time.ITimeService.MILISECONDS_IN_MINUTE;
import static org.mockito.Mockito.*;

public class TransactionServiceTests {
    @Test
    public void givenTransactionInThePast_createTransactionReturnsFalse(){
        long now  = 100000;
        long transactionTime = now - (MILISECONDS_IN_MINUTE*2);
        testResponse(now,transactionTime,false);
    }

    @Test
    public void givenTransactionInTheFuture_createTransactionReturnsFalse(){
        long now  = 100000;
        long transactionTime = now;
        testResponse(now,transactionTime,true);
    }

    @Test
    public void givenAnyTransaction_createTransaction_raisesCorrectEvent(){
        double anyAmount = 12.0;
        long anyTimeStamp = 123123;
        ITimeService timeService = mock(ITimeService.class);
        TransactionMadeEventHandler handler = spy(new TransactionMadeEventHandler());
        when(timeService.getUtcNow()).thenAnswer(x->1999l);
        TransactionDTO transaction = new TransactionDTO(anyAmount, anyTimeStamp);
        TransactionsService sut = new TransactionsService(handler,timeService);

        sut.createTransaction(transaction);

        verify(handler).publish(eq(new Transaction(anyAmount,anyTimeStamp)));
    }

    private void testResponse(long now,long transactionTimestamp, boolean expectedVal) {
        Double anyAmount = 12.0;
        ITimeService timeService = mock(ITimeService.class);
        TransactionMadeEventHandler handler = spy(new TransactionMadeEventHandler());
        when(timeService.getUtcNow()).thenAnswer(x->now);
        TransactionDTO oldTransaction = new TransactionDTO(anyAmount, transactionTimestamp);
        TransactionsService sut = new TransactionsService(handler,timeService);

        boolean isNew = sut.createTransaction(oldTransaction);
        Assert.assertEquals(expectedVal,isNew);
    }


}
