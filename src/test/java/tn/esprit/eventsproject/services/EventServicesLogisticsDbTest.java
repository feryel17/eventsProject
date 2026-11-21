package tn.esprit.eventsproject.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import tn.esprit.eventsproject.entities.Event;
import tn.esprit.eventsproject.entities.Logistics;
import tn.esprit.eventsproject.repositories.EventRepository;
import tn.esprit.eventsproject.repositories.LogisticsRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests SANS MOCK : vrais repositories, vraie base (H2 en mémoire).
 * Chaque test est annulé (rollback) à la fin, la base repart propre.
 */
@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(EventServicesImpl.class)
class EventServicesLogisticsDbTest {

    @Autowired
    private EventServicesImpl eventServices;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private LogisticsRepository logisticsRepository;

    private Logistics newLogistics(boolean reserve, float prix, int quantite) {
        Logistics l = new Logistics();
        l.setDescription("Chaises");
        l.setReserve(reserve);
        l.setPrixUnit(prix);
        l.setQuantite(quantite);
        return l;
    }

    @Test
    void addAffectLog_enregistreLaLogistiqueEnBase() {
        Event event = new Event();
        event.setDescription("Conference");
        event.setDateDebut(LocalDate.of(2026, 6, 1));
        eventRepository.save(event);

        Logistics saved = eventServices.addAffectLog(newLogistics(true, 10f, 2), "Conference");

        assertNotNull(saved);
        assertTrue(saved.getIdLog() > 0);
        assertEquals(1, logisticsRepository.count());
    }

    @Test
    void addAffectLog_evenementInconnu_retourneNull() {
        Logistics result = eventServices.addAffectLog(newLogistics(true, 10f, 2), "Inconnu");

        assertNull(result);
        assertEquals(0, logisticsRepository.count());
    }

    @Test
    void getLogisticsDates_retourneLesLogistiquesReserveesDeLaPeriode() {
        Logistics reservee = logisticsRepository.save(newLogistics(true, 10f, 2));
        Logistics nonReservee = logisticsRepository.save(newLogistics(false, 20f, 1));

        Event event = new Event();
        event.setDescription("Conference");
        event.setDateDebut(LocalDate.of(2026, 6, 1));
        HashSet<Logistics> set = new HashSet<>();
        set.add(reservee);
        set.add(nonReservee);
        event.setLogistics(set);
        eventRepository.save(event);

        List<Logistics> result = eventServices.getLogisticsDates(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isReserve());
    }

    @Test
    void getLogisticsDates_evenementSansLogistique_retourneNull() {
        Event event = new Event();
        event.setDescription("Vide");
        event.setDateDebut(LocalDate.of(2026, 6, 1));
        event.setLogistics(new HashSet<>());
        eventRepository.save(event);

        List<Logistics> result = eventServices.getLogisticsDates(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertNull(result);
    }

    @Test
    void getLogisticsDates_horsPeriode_retourneListeVide() {
        Event event = new Event();
        event.setDescription("Ancien");
        event.setDateDebut(LocalDate.of(2020, 1, 1));
        event.setLogistics(new HashSet<>());
        eventRepository.save(event);

        List<Logistics> result = eventServices.getLogisticsDates(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}