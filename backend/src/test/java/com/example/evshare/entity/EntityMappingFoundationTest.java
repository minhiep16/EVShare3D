package com.example.evshare.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EntityMappingFoundationTest {

    @Autowired
    private EntityManager entityManager;

    private static final List<Class<?>> EXPECTED_ENTITY_CLASSES = List.of(
            User.class,
            Role.class,
            UserRole.class,
            IdentityVerification.class,
            DriverLicense.class,
            Vehicle.class,
            OwnershipGroup.class,
            OwnershipShare.class,
            CoOwnershipContract.class,
            ContractSignature.class,
            Booking.class,
            UsageSession.class,
            VehicleInspection.class,
            VehicleService.class,
            SharedFund.class,
            FundTransaction.class,
            Expense.class,
            ExpenseAllocation.class,
            Payment.class,
            Proposal.class,
            VoteOption.class,
            Vote.class,
            Dispute.class,
            DisputeEvidence.class,
            Notification.class,
            AiRecommendation.class,
            AuditLog.class
    );

    @Test
    @DisplayName("EntityManager should be injected and JPA Metamodel should be active")
    void testEntityManagerAndMetamodelActive() {
        assertNotNull(entityManager, "EntityManager must be injected");
        Metamodel metamodel = entityManager.getMetamodel();
        assertNotNull(metamodel, "JPA Metamodel must not be null");
    }

    @Test
    @DisplayName("All 27 required domain entity classes must be registered as JPA Entities in Metamodel")
    void testAll27EntitiesRegisteredInMetamodel() {
        Metamodel metamodel = entityManager.getMetamodel();
        Set<Class<?>> managedEntityJavaTypes = metamodel.getEntities().stream()
                .map(EntityType::getJavaType)
                .collect(Collectors.toSet());

        assertEquals(27, EXPECTED_ENTITY_CLASSES.size(), "Specification requires exactly 27 persistence entities");

        for (Class<?> entityClass : EXPECTED_ENTITY_CLASSES) {
            assertTrue(managedEntityJavaTypes.contains(entityClass),
                    "Entity class " + entityClass.getSimpleName() + " must be registered in JPA metamodel");
            assertTrue(entityClass.isAnnotationPresent(Entity.class),
                    "Entity class " + entityClass.getSimpleName() + " must have @Entity annotation");
            assertTrue(entityClass.isAnnotationPresent(Table.class),
                    "Entity class " + entityClass.getSimpleName() + " must have @Table annotation");
        }
    }

    @Test
    @DisplayName("Each entity must have an identified primary key attribute")
    void testEntityPrimaryKeysIdentified() {
        Metamodel metamodel = entityManager.getMetamodel();

        for (Class<?> entityClass : EXPECTED_ENTITY_CLASSES) {
            EntityType<?> entityType = metamodel.entity(entityClass);
            assertNotNull(entityType, "EntityType must exist for " + entityClass.getSimpleName());

            if (entityClass.equals(UserRole.class)) {
                // UserRole uses composite primary key via @IdClass(UserRoleId.class)
                assertFalse(entityType.hasSingleIdAttribute(), "UserRole should have composite ID attributes");
                assertNotNull(entityType.getIdClassAttributes(), "UserRole must have idClassAttributes");
                assertEquals(2, entityType.getIdClassAttributes().size(), "UserRole should have 2 id attributes (user, role)");
            } else {
                // All other 26 entities use single Long id
                assertTrue(entityType.hasSingleIdAttribute(), entityClass.getSimpleName() + " must have single ID attribute");
                assertNotNull(entityType.getId(Long.class),
                        entityClass.getSimpleName() + " must have primary key attribute 'id' of type Long");
            }
        }
    }

    @Test
    @DisplayName("Verify representative table names correspond to DATABASE.md specification")
    void testTableNamesMatchSpecification() {
        assertEquals("users", User.class.getAnnotation(Table.class).name());
        assertEquals("vehicles", Vehicle.class.getAnnotation(Table.class).name());
        assertEquals("ownership_groups", OwnershipGroup.class.getAnnotation(Table.class).name());
        assertEquals("co_ownership_contracts", CoOwnershipContract.class.getAnnotation(Table.class).name());
        assertEquals("bookings", Booking.class.getAnnotation(Table.class).name());
        assertEquals("shared_funds", SharedFund.class.getAnnotation(Table.class).name());
        assertEquals("expenses", Expense.class.getAnnotation(Table.class).name());
        assertEquals("proposals", Proposal.class.getAnnotation(Table.class).name());
        assertEquals("disputes", Dispute.class.getAnnotation(Table.class).name());
        assertEquals("notifications", Notification.class.getAnnotation(Table.class).name());
        assertEquals("audit_logs", AuditLog.class.getAnnotation(Table.class).name());
    }
}
