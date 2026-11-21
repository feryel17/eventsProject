package tn.esprit.eventsproject.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.esprit.eventsproject.entities.Event;
import tn.esprit.eventsproject.entities.Logistics;
import tn.esprit.eventsproject.entities.Participant;
import tn.esprit.eventsproject.entities.Tache;
import tn.esprit.eventsproject.repositories.EventRepository;
import tn.esprit.eventsproject.repositories.LogisticsRepository;
import tn.esprit.eventsproject.repositories.ParticipantRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Slf4j
@RequiredArgsConstructor
@Service
public class EventServicesImpl implements IEventServices{

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final LogisticsRepository logisticsRepository;

    @Override
    public Participant addParticipant(Participant participant) {
        log.info("Ajout du participant : {} {}", participant.getNom(), participant.getPrenom());
        Participant saved = participantRepository.save(participant);
        log.debug("Participant enregistré, id = {}", saved.getIdPart());
        return saved;
    }

    @Override
    public Event addAffectEvenParticipant(Event event, int idParticipant) {
        log.info("Affectation de l'événement '{}' au participant {}", event.getDescription(), idParticipant);
        Participant participant = participantRepository.findById(idParticipant).orElse(null);
        if (participant == null) {
            log.error("Participant introuvable, id = {}", idParticipant);
            throw new IllegalArgumentException("Participant introuvable : " + idParticipant);
        }
        if (participant.getEvents() == null) {
            log.debug("Le participant {} n'a pas encore d'événements : création de la liste", idParticipant);
            Set<Event> events = new HashSet<>();
            events.add(event);
            participant.setEvents(events);
        } else {
            participant.getEvents().add(event);
        }
        return eventRepository.save(event);
    }

    @Override
    public Event addAffectEvenParticipant(Event event) {
        Set<Participant> participants = event.getParticipants();
        for(Participant aParticipant:participants){
            Participant participant = participantRepository.findById(aParticipant.getIdPart()).orElse(null);
            if(participant.getEvents() == null){
                Set<Event> events = new HashSet<>();
                events.add(event);
                participant.setEvents(events);
            }else {
                participant.getEvents().add(event);
            }
        }
        return eventRepository.save(event);
    }

    @Override
    public Logistics addAffectLog(Logistics logistics, String descriptionEvent) {
        log.info("addAffectLog : ajout d'une logistique à l'événement '{}'", descriptionEvent);
        log.debug("Logistique : prixUnit={}, quantite={}", logistics.getPrixUnit(), logistics.getQuantite());

        Event event = eventRepository.findByDescription(descriptionEvent);
        if (event == null) {
            log.error("addAffectLog : aucun événement trouvé avec la description '{}'", descriptionEvent);
            return null;
        }

        if (event.getLogistics() == null) {
            log.debug("L'événement '{}' n'a pas encore de logistique", descriptionEvent);
            Set<Logistics> logisticsSet = new HashSet<>();
            logisticsSet.add(logistics);
            event.setLogistics(logisticsSet);
            eventRepository.save(event);
        } else {
            log.debug("L'événement '{}' a déjà {} logistique(s)", descriptionEvent, event.getLogistics().size());
            event.getLogistics().add(logistics);
        }
        Logistics saved = logisticsRepository.save(logistics);
        log.info("addAffectLog : logistique enregistrée pour l'événement '{}'", descriptionEvent);
        return saved;
    }

    @Override
    public List<Logistics> getLogisticsDates(LocalDate date_debut, LocalDate date_fin) {
        log.info("getLogisticsDates : recherche entre {} et {}", date_debut, date_fin);
        List<Event> events = eventRepository.findByDateDebutBetween(date_debut, date_fin);
        log.debug("{} événement(s) trouvé(s) dans cette période", events.size());

        List<Logistics> logisticsList = new ArrayList<>();
        for (Event event : events) {
            if (event.getLogistics().isEmpty()) {
                log.warn("L'événement '{}' n'a aucune logistique, retour null", event.getDescription());
                return null;
            } else {
                Set<Logistics> logisticsSet = event.getLogistics();
                for (Logistics logistics : logisticsSet) {
                    if (logistics.isReserve())
                        logisticsList.add(logistics);
                }
            }
        }
        log.info("getLogisticsDates : {} logistique(s) réservée(s) trouvée(s)", logisticsList.size());
        return logisticsList;
    }

    @Scheduled(cron = "*/60 * * * * *")
    @Override
    public void calculCout() {
        log.info("calculCout : début du calcul des coûts");
        List<Event> events = eventRepository.findByParticipants_NomAndParticipants_PrenomAndParticipants_Tache("Tounsi","Ahmed", Tache.ORGANISATEUR);
        log.debug("{} événement(s) à traiter", events.size());
        float somme = 0f;
        for(Event event:events){
            log.info(event.getDescription());
            Set<Logistics> logisticsSet = event.getLogistics();
            for (Logistics logistics:logisticsSet){
                if(logistics.isReserve())
                    somme+=logistics.getPrixUnit()*logistics.getQuantite();
            }
            event.setCout(somme);
            eventRepository.save(event);
            log.info("Cout de l'Event "+event.getDescription()+" est "+ somme);
        }
        log.info("calculCout : fin du calcul");
    }

}