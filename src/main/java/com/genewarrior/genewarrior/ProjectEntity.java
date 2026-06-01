package com.genewarrior.genewarrior;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Date;

@Entity(name = "project")
@Table(name = "project")
public class ProjectEntity implements Serializable {
    @Getter
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long identity;

    @Getter
    @Setter
    private String link;

    @Getter
    @Setter
    @Column(columnDefinition = "text")
    private String text;

    @Getter
    @Setter
    @Column(name = "upload_date")
    private Date uploadDate;


}
