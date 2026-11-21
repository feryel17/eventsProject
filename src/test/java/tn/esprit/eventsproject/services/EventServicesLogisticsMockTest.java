package tn.esprit.eventsproject.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.eventsproject.entities.Event;
import tn.esprit.eventsproject.entities.Logistics;
import tn.esprit.eventsproject.entities.Tache;
import tn.esprit.eventsproject.repositories.EventRepository;
import tn.esprit.eventsproject.repositories.LogisticsRepository;
import tn.esprit.eventsproject.repositories.ParticipantRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests AVEC MOCK : les repositories sont simulés par Mockito,
 * aucune base de données n'est utilisée.
 */
@ExtendWith(MockitoExtension.class)
class EventServicesLogisticsMockTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private LogisticsRepository logisticsRepository;

    @InjectMocks
    private EventServicesImpl eventServices;

    // ---------- Méthode utilitaire ----------
    private Logistics newLogistics(boolean reserve, float prix, int quantite) {
        Logistics l = new Logistics();
        l.setDescription("Chaises");
        l.setReserve(reserve);
        l.setPrixUnit(prix);
        l.setQuantite(quantite);
        return l;
    }

    // ================= addAffectLog =================

    @Test
    void addAffectLog_evenementSansLogistique_creeLEnsembleEtSauvegarde() {
        Event event = new Event();
        event.setDescription("Conference");
        event.setLogistics(null);
        Logistics logistics = newLogistics(true, 10f, 2);

        when(eventRepository.findByDescription("Conference")).thenReturn(event);
        when(logisticsRepository.save(logistics)).thenReturn(logistics);

        Logistics result = eventServices.addAffectLog(logistics, "Conference");

        assertNotNull(result);
        assertEquals(1, event.getLogistics().size());
        verify(eventRepository, times(1)).save(event);
        verify(logisticsRepository, times(1)).save(logistics);
    }

    @Test
    void addAffectLog_evenementAvecLogistique_ajouteALEnsemble() {
        Event event = new Event();
        event.setDescription("Conference");
        Set<Logistics> existing = new HashSet<>();
        existing.add(newLogistics(true, 5f, 1));
        event.setLogistics(existing);
        Logistics logistics = newLogistics(true, 10f, 2);

        when(eventRepository.findByDescription("Conference")).thenReturn(event);
        when(logisticsRepository.save(logistics)).thenReturn(logistics);

        Logistics result = eventServices.addAffectLog(logistics, "Conference");

        assertNotNull(result);
        assertEquals(2, event.getLogistics().size());
        verify(eventRepository, never()).save(any(Event.class));
        verify(logisticsRepository, times(1)).save(logistics);
    }

    @Test
    void addAffectLog_evenementIntrouvable_retourneNull() {
        when(eventRepository.findByDescription("Inconnu")).thenReturn(null);

        Logistics result = eventServices.addAffectLog(newLogistics(true, 10f, 2), "Inconnu");

        assertNull(result);
        verify(logisticsRepository, never()).save(any(Logistics.class));
    }

    // ================= getLogisticsDates =================

    @Test
    void getLogisticsDates_retourneSeulementLesLogistiquesReservees() {
        LocalDate debut = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 12, 31);

        Logistics reservee = newLogistics(true, 10f, 2);
        Logistics nonReservee = newLogistics(false, 20f, 1);
        Event event = new Event();
        event.setDescription("Conference");
        event.setLogistics(new HashSet<>(Arrays.asList(reservee, nonReservee)));

        when(eventRepository.findByDateDebutBetween(debut, fin))
                .thenReturn(Collections.singletonList(event));

        List<Logistics> result = eventServices.getLogisticsDates(debut, fin);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isReserve());
    }

    @Test
    void getLogisticsDates_evenementSansLogistique_retourneNull() {
        LocalDate debut = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 12, 31);

        Event event = new Event();
        event.setDescription("Vide");
        event.setLogistics(new HashSet<>());

        when(eventRepository.findByDateDebutBetween(debut, fin))
                .thenReturn(Collections.singletonList(event));

        assertNull(eventServices.getLogisticsDates(debut, fin));
    }

    @Test
    void getLogisticsDates_aucunEvenement_retourneListeVide() {
        LocalDate debut = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 12, 31);

        when(eventRepository.findByDateDebutBetween(debut, fin))
                .thenReturn(Collections.emptyList());

        List<Logistics> result = eventServices.getLogisticsDates(debut, fin);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ================= calculCout =================

    @Test
    void calculCout_sommeSeulementLesLogistiquesReservees() {
        Event event = new Event();
        event.setDescription("Conference");
        // 10 x 2 = 20 (réservée) ; 100 x 1 = 100 (non réservée, ignorée)
        event.setLogistics(new HashSet<>(Arrays.asList(
                newLogistics(true, 10f, 2),
                newLogistics(false, 100f, 1))));

        when(eventRepository.findByParticipants_NomAndParticipants_PrenomAndParticipants_Tache(
                "Tounsi", "Ahmed", Tache.ORGANISATEUR))
                .thenReturn(Collections.singletonList(event));

        eventServices.calculCout();

        assertEquals(20f, event.getCout(), 0.001f);
        verify(eventRepository, times(1)).save(event);
    }
}