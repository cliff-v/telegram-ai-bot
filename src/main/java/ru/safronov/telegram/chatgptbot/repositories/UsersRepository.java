package ru.safronov.telegram.chatgptbot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.safronov.telegram.chatgptbot.models.User;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<User, Long> {
    Optional<User> findByTgId(Long tgId);
}
