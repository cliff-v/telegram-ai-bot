package ru.safronov.telegram.chatgptbot.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.safronov.telegram.chatgptbot.models.User;

import java.util.List;

@DataJpaTest
class UsersRepositoryTest {

    @Autowired
    private UsersRepository usersRepository;

    @Test
    public void testFindAll() {
        List<User> users = usersRepository.findAll();
        var user = usersRepository.findById(1L);
        System.out.println(users);
        System.out.println(user);
    }
}