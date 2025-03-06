package uk.gov.dwp.uc.pairtest;

import java.util.EnumMap;
import java.util.Map;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
    
	private static final int MAX_TICKETS_PER_BOOKING = 25;
	private static final int ADULT_TICKET_PRICE = 25;
	private static final int CHILD_TICKET_PRICE = 15;
	
	private final TicketPaymentService ticketPaymentService;
	private final SeatReservationService seatReservationService;
	

    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
		this.ticketPaymentService = ticketPaymentService;
		this.seatReservationService = seatReservationService;
	}
    
	@Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
		
		if (accountId == null || accountId <= 0) {
			throw new InvalidPurchaseException("Invalid account ID.");
		}
		
		if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
			throw new InvalidPurchaseException("No tickets selected for the booking.");
		}
		
		Map<TicketTypeRequest.Type, Integer> ticketCounts = countTickets(ticketTypeRequests);
		validateTicketRules(ticketCounts);
		
		int totalAmountToPay = (ticketCounts.getOrDefault(TicketTypeRequest.Type.ADULT, 0) * ADULT_TICKET_PRICE) +
	               (ticketCounts.getOrDefault(TicketTypeRequest.Type.CHILD, 0) * CHILD_TICKET_PRICE);
		
		int totalSeatstoAllocate = ticketCounts.getOrDefault(TicketTypeRequest.Type.ADULT, 0) +
	               ticketCounts.getOrDefault(TicketTypeRequest.Type.CHILD, 0);
		
		ticketPaymentService.makePayment(accountId, totalAmountToPay);
		seatReservationService.reserveSeat(accountId, totalSeatstoAllocate);
		
    }
	
	private Map<TicketTypeRequest.Type, Integer> countTickets(TicketTypeRequest... ticketTypeRequests) {
		
		Map<TicketTypeRequest.Type, Integer> ticketCounts = new EnumMap<>(TicketTypeRequest.Type.class);
		
		for (TicketTypeRequest ticketRequest : ticketTypeRequests) {
			ticketCounts.put(ticketRequest.getTicketType(), ticketCounts.getOrDefault(ticketRequest.getTicketType(), 0) + ticketRequest.getNoOfTickets());
		}
		
		return ticketCounts;
	}
	
	private void validateTicketRules(Map<TicketTypeRequest.Type, Integer> ticketCounts) {
		
		int totalTickets = ticketCounts.values().stream().mapToInt(Integer::intValue).sum();
		int adultTickets = ticketCounts.getOrDefault(TicketTypeRequest.Type.ADULT, 0);
		int childTickets = ticketCounts.getOrDefault(TicketTypeRequest.Type.CHILD, 0);
		int infantTickets = ticketCounts.getOrDefault(TicketTypeRequest.Type.INFANT, 0);
		
		if (totalTickets == 0) {
			throw new InvalidPurchaseException("No tickets booked.");
		}
		
		if (totalTickets > MAX_TICKETS_PER_BOOKING) {
			throw new InvalidPurchaseException("Invalid number of tickets. Maximum tickets allowed per booking is " +MAX_TICKETS_PER_BOOKING+ ".");
		}
		
		if ((childTickets > 0 || infantTickets > 0) && adultTickets == 0) {
			throw new InvalidPurchaseException("Child and Infant tickets must be accompanied with at least one Adult ticket.");
		}
	}
}
