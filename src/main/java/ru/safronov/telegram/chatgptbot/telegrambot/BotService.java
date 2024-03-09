package ru.safronov.telegram.chatgptbot.telegrambot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.safronov.telegram.chatgptbot.models.UserType;
import ru.safronov.telegram.chatgptbot.repositories.UsersRepository;

@Service
@RequiredArgsConstructor
public class BotService {
    private final UsersRepository usersRepository;
    public static boolean checkAccess(ru.safronov.telegram.chatgptbot.models.User userDao) {
        return userDao.getType().equals(UserType.ADMIN.name()) || userDao.getType().equals(UserType.USER.name());
    }

    public ru.safronov.telegram.chatgptbot.models.User fillAndUpdateUsername(
            User user,
            ru.safronov.telegram.chatgptbot.models.User userDao
    ) {
        userDao.setTgName(user.getUserName());
        userDao.setFirstName(user.getFirstName());
        userDao.setLastName(user.getLastName());

        return usersRepository.save(userDao);
    }
}
