package com.gatekeeper.management.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Table(name = "organizations")
@Entity
@Setter
@Getter
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String slug;

    @Enumerated(EnumType.STRING)
    private OrganizationStatus status;

    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;
}
