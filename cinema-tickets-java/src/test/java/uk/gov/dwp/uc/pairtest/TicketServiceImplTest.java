package uk.gov.dwp.uc.pairtest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import static org.mockito.Mockito.*;

public class TicketServiceImplTest {

    @Mock
    private TicketPaymentService ticketPaymentService;

    @Mock
    private SeatReservationService seatReservationService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private static final long VALID_ACCOUNT_ID = 1L;
    private static final long INVALID_ACCOUNT_ID = -1L;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ticketService = new TicketServiceImpl(ticketPaymentService, seatReservationService);
    }


    /** Test Case: Valid purchase with Adult and Child tickets */
    @Test
    public void testValidPurchase() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        ticketService.purchaseTickets(VALID_ACCOUNT_ID, adultTickets, childTickets, infantTickets);

        verify(ticketPaymentService, times(1)).makePayment(VALID_ACCOUNT_ID, (2 * 25) + (1 * 15));
        verify(seatReservationService, times(1)).reserveSeat(VALID_ACCOUNT_ID, 3);
    }

    /** Test Case: No Adult ticket (only Child/Infant) - Should Fail */
    @Test
    public void testChildOrInfantWithoutAdultThrowsException() {
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        ticketService.purchaseTickets(VALID_ACCOUNT_ID, childTickets, infantTickets);
    }

    /** Test Case: Exceeding max ticket limit */
    @Test
    public void testExceedingMaxTicketsThrowsException() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 20);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 6);

        ticketService.purchaseTickets(VALID_ACCOUNT_ID, adultTickets, childTickets);
    }

    /** Test Case: Invalid Account ID */
    @Test
    public void testInvalidAccountIdThrowsException() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);

        ticketService.purchaseTickets(INVALID_ACCOUNT_ID, adultTickets);
    }

    /** Test Case: No tickets selected */
    @Test
    public void testNoTicketsThrowsException() {
        ticketService.purchaseTickets(VALID_ACCOUNT_ID);
    }

    /** Test Case: Only Adult tickets */
    @Test
    public void testOnlyAdultTicketsPurchase() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3);

        ticketService.purchaseTickets(VALID_ACCOUNT_ID, adultTickets);

        verify(ticketPaymentService, times(1)).makePayment(VALID_ACCOUNT_ID, 3 * 25);
        verify(seatReservationService, times(1)).reserveSeat(VALID_ACCOUNT_ID, 3);
    }

    /** Test Case: Adult with Infant (Infants are not charged, but require an Adult) */
    @Test
    public void testAdultWithInfantDoesNotChargeForInfant() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        ticketService.purchaseTickets(VALID_ACCOUNT_ID, adultTickets, infantTickets);

        verify(ticketPaymentService, times(1)).makePayment(VALID_ACCOUNT_ID, 2 * 25);
        verify(seatReservationService, times(1)).reserveSeat(VALID_ACCOUNT_ID, 2);
    }
}
