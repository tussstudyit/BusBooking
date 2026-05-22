package com.example.busbooking.admin.service;

import com.example.busbooking.admin.model.RouteDto;
import com.example.busbooking.admin.model.RouteForm;
import com.example.busbooking.admin.model.RouteCatalogEntry;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RouteAdminService {
    private static final Logger log = LoggerFactory.getLogger(RouteAdminService.class);
    private static final int DEFAULT_ROUTE_LIMIT = 200;

    private final Firestore firestore;
    private final FirestoreIdService idService;
    private final RouteCatalogService routeCatalogService;

    public RouteAdminService(
            Firestore firestore,
            FirestoreIdService idService,
            RouteCatalogService routeCatalogService
    ) {
        this.firestore = firestore;
        this.idService = idService;
        this.routeCatalogService = routeCatalogService;
    }

    public List<RouteDto> findAll() {
        try {
            return firestore.collection("routes")
                    .limit(DEFAULT_ROUTE_LIMIT)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(this::toDto)
                    .sorted((a, b) -> (a.origin() + a.destination()).compareToIgnoreCase(b.origin() + b.destination()))
                    .toList();
        } catch (Exception e) {
            log.warn("Could not load routes from Firestore", e);
            throw new IllegalStateException("Could not load routes", e);
        }
    }

    public RouteDto findByDocumentId(String documentId) {
        try {
            DocumentSnapshot document = firestore.collection("routes").document(documentId).get().get();
            if (!document.exists()) {
                throw new IllegalArgumentException("Route not found");
            }
            return toDto(document);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load route", e);
        }
    }

    public void create(RouteForm form) {
        long id = idService.nextNumericId("routes");
        Map<String, Object> data = new HashMap<>();
        RouteCatalogEntry routeInfo = routeInfo(form);
        data.put("id", id);
        data.putAll(toRouteMap(form, routeInfo));
        data.put("isActive", form.getIsActive() == null || form.getIsActive());
        data.put("createdAt", System.currentTimeMillis());
        try {
            firestore.collection("routes").document(String.valueOf(id)).set(data).get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not create route", e);
        }
    }

    public void update(String documentId, RouteForm form) {
        Map<String, Object> data = new HashMap<>();
        data.putAll(toRouteMap(form, routeInfo(form)));
        data.put("isActive", form.getIsActive() == null || form.getIsActive());
        try {
            firestore.collection("routes").document(documentId).update(data).get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not update route", e);
        }
    }

    public void setActive(String documentId, boolean active) {
        try {
            firestore.collection("routes").document(documentId).update("isActive", active).get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not update route status", e);
        }
    }

    private RouteDto toDto(DocumentSnapshot document) {
        String origin = document.getString("origin");
        String destination = document.getString("destination");
        String originId = stringOrInferred(document, "originId", origin);
        String destinationId = stringOrInferred(document, "destinationId", destination);
        return new RouteDto(
                document.getId(),
                FirestoreMapper.longValue(document, "id"),
                originId,
                destinationId,
                origin,
                destination,
                FirestoreMapper.intValue(document, "distance"),
                FirestoreMapper.longValue(document, "suggestedPrice"),
                FirestoreMapper.longValue(document, "durationMs"),
                document.getBoolean("isActive"),
                FirestoreMapper.longValue(document, "createdAt")
        );
    }

    private Map<String, Object> toRouteMap(RouteForm form, RouteCatalogEntry routeInfo) {
        Map<String, Object> data = new HashMap<>();
        String originId = form.getOriginId().trim();
        String destinationId = form.getDestinationId().trim();
        data.put("originId", originId);
        data.put("destinationId", destinationId);
        data.put("origin", routeCatalogService.provinceName(originId));
        data.put("destination", routeCatalogService.provinceName(destinationId));
        data.put("distance", form.getDistance() != null ? form.getDistance() : routeInfo.distanceKm());
        data.put("suggestedPrice", routeInfo.price());
        data.put("durationMs", routeInfo.durationMs());
        return data;
    }

    private RouteCatalogEntry routeInfo(RouteForm form) {
        String originId = form.getOriginId().trim();
        String destinationId = form.getDestinationId().trim();
        if (originId.equals(destinationId)) {
            throw new IllegalArgumentException("Origin and destination must be different");
        }
        Optional<RouteCatalogEntry> catalogEntry = routeCatalogService.findRoute(originId, destinationId);
        return catalogEntry.orElseGet(() -> new RouteCatalogEntry(
                originId,
                routeCatalogService.provinceName(originId),
                destinationId,
                routeCatalogService.provinceName(destinationId),
                form.getDistance() == null ? 1 : form.getDistance(),
                4 * 3_600_000L,
                routeCatalogService.price(originId, destinationId)
        ));
    }

    private String stringOrInferred(DocumentSnapshot document, String field, String provinceName) {
        String value = document.getString(field);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return routeCatalogService.provinceIdByName(provinceName).orElse(null);
    }
}
