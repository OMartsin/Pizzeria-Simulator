package com.example.pizzeria;

import com.example.pizzeria.managers.cashregister.CashRegisterManager;
import com.example.pizzeria.config.PizzeriaConfig;
import com.example.pizzeria.models.Diner;
import com.example.pizzeria.models.Order;
import com.example.pizzeria.models.Recipe;
import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class DinersGenerator {
    private PizzeriaConfig pizzeriaConfig;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> taskHandle;
    private final CashRegisterManager cashRegisterManager;

    public void setDinerArrivalConfig(PizzeriaConfig pizzeriaConfig) {
        this.pizzeriaConfig = pizzeriaConfig;
    }

    public void start() throws IllegalStateException {
        checkArrivalConfigNotNull();

        Runnable generateDiners = () -> {
            System.out.println(pizzeriaConfig.getDinerArrivalConfig().getQuantity());
            for (int i = 0; i < pizzeriaConfig.getDinerArrivalConfig().getQuantity(); ++i) {
                Diner newDiner = generateDinner(pizzeriaConfig.getMenu());
                cashRegisterManager.acceptDinner(newDiner);
                //System.out.println("Dinner " + newDinner.name() +  " order" + newDinner.order());
            }

        };
        taskHandle = scheduler.scheduleAtFixedRate(generateDiners,
                0,
                pizzeriaConfig.getDinerArrivalConfig().getFrequency().value,
                TimeUnit.SECONDS);

    }

    public void pause() throws IllegalStateException {
        checkArrivalConfigNotNull();

        taskHandle.cancel(false);
    }

    private void checkArrivalConfigNotNull() throws IllegalStateException {
        if (pizzeriaConfig.getDinerArrivalConfig() == null) {
            throw new IllegalStateException("DinerArrivalConfig is not initialized");
        }
    }

    private Diner generateDinner(List<Recipe> menu) {
        int random = (int) (Math.random() * menu.size() - 1) + 1;
        List<Recipe> tempList = new ArrayList<>(menu);
        Collections.shuffle(tempList, new Random()); // Shuffle the list randomly
        Faker faker = new Faker();
        return new Diner(faker.name().fullName(), new Order(null, tempList.subList(0,random), null));
    }

}
