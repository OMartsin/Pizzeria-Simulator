package com.example.pizzeria.event_listeners;

import com.example.pizzeria.dto.*;
import com.example.pizzeria.events.*;
import com.example.pizzeria.managers.cashregister.CashRegister;
import com.example.pizzeria.models.Order;
import com.example.pizzeria.models.PizzaCookingState;
import com.example.pizzeria.models.PizzaStage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Component
@RequiredArgsConstructor
public class NetworkEventListener implements UpdateEventListener{

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @EventListener
    public void update(UpdateEvent event) {
        if (event instanceof ServiceOrderUpdateEvent) {
            handleServiceOrderUpdateEvent((ServiceOrderUpdateEvent) event);
        } else if (event instanceof PausedCookUpdateEvent) {
            handlePausedCookUpdateEvent((PausedCookUpdateEvent) event);
        } else if (event instanceof PostCookingOrderUpdateEvent) {
            handlePostCookingOrderUpdateEvent((PostCookingOrderUpdateEvent) event);
        } else if (event instanceof PreCookingOrderUpdateEvent) {
            handlePreCookingOrderUpdateEvent((PreCookingOrderUpdateEvent) event);
        }else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported UpdateEvent type");
        }
    }


    private void handleServiceOrderUpdateEvent(ServiceOrderUpdateEvent event) {
        String destination = "/topic/newOrder";
        CashRegister cashRegister = event.getCashRegister();
        Order order = event.getOrder();
        DinerDto dinerDto = new DinerDto(order.getDiner().getId(), order.getDiner().getName());
        List<OrderPizzaDto> orderPizzaDtos = order.getOrderedItems().stream()
                .map(orderedItem -> new OrderPizzaDto(orderedItem.getId(), order.getId(),
                        orderedItem.getRecipe().getId()))
                .toList();
        ServiceOrderDto dto = new ServiceOrderDto(
                Long.valueOf(order.getId()),
                Long.valueOf(cashRegister.getId()),
                order.getOrderTime(),
                orderPizzaDtos,
                dinerDto
                );
        messagingTemplate.convertAndSend(destination, dto);
    }

    private void handlePausedCookUpdateEvent(PausedCookUpdateEvent event) {
        String destination = "/topic/pausedCookUpdate";

        // Create a PauseCookDto with the cookId
        PauseCookDto dto = new PauseCookDto(event.getCook().getCookId(), event.getCook().getStatus());
        messagingTemplate.convertAndSend(destination, dto);
    }

    private void handlePreCookingOrderUpdateEvent(PreCookingOrderUpdateEvent event) {
        String destination = "/topic/cookingOrderUpdate";
        PizzaCookingState pizzaCookingState = event.getPizzaCookingState();
        String topping;
        if(pizzaCookingState.getCurrPizzaStage().equals(PizzaStage.Topping)){
            topping = pizzaCookingState.getNextTopping();
        }else {
            topping = null;
        }
        Integer cookId = event.getCook().getCookId();
        Integer orderId = pizzaCookingState.getOrderId();

        // Create a CookingOrderDto with relevant information
        CookingOrderDto dto = new CookingOrderDto
                (pizzaCookingState.getCurrPizzaStage(), topping, cookId, orderId,
                        pizzaCookingState.getOrderedItem().getId(),
                        pizzaCookingState.getCompletedAt(), pizzaCookingState.getModifiedAt());

        messagingTemplate.convertAndSend(destination, dto);
    }

    private void handlePostCookingOrderUpdateEvent(PostCookingOrderUpdateEvent event) {
        String destination = "/topic/cookingOrderUpdate";
        PizzaCookingState pizzaCookingState = event.getPizzaCookingState();
        String topping = pizzaCookingState.getCurrentTopping();
        Integer cookId = event.getCook().getCookId();
        Integer orderId = pizzaCookingState.getOrderId();

        // Create a CookingOrderDto with relevant information
        CookingOrderDto dto = new CookingOrderDto
                (pizzaCookingState.getCurrPizzaStage(), topping, cookId, orderId,
                        pizzaCookingState.getOrderedItem().getId(),
                        pizzaCookingState.getCompletedAt(), pizzaCookingState.getModifiedAt());

        messagingTemplate.convertAndSend(destination, dto);
    }
}
