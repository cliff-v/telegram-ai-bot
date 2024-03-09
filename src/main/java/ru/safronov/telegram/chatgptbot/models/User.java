package ru.safronov.telegram.chatgptbot.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User extends BaseSequenceEntity {
    private String tgName;
    private String comment;
    private String type;
    private String realName;
    private Long tgId;
    private String firstName;
    private String lastName;
}