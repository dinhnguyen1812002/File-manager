package com.app.file_transfer.config;

import com.app.file_transfer.model.PersistentLogins;
import com.app.file_transfer.repository.PersistentLoginRepository;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
@AllArgsConstructor
@org.springframework.transaction.annotation.Transactional
public class JpaPersistentTokenRepository implements PersistentTokenRepository {

    private final PersistentLoginRepository persistentLoginRepository;

    @Override
    public void createNewToken(PersistentRememberMeToken token) {

        PersistentLogins pl = new PersistentLogins();
        pl.setUsername(token.getUsername());
        pl.setSeries(token.getSeries());
        pl.setToken(token.getTokenValue());
        pl.setLastUsed(converDateToLocalDateTime(token.getDate()));
        persistentLoginRepository.save(pl);
    }

    @Override
    public void updateToken(String series, String tokenValue, Date lastUsed) {

        persistentLoginRepository.findById(series).ifPresent(pl -> {
            pl.setToken(tokenValue);
            pl.setLastUsed(converDateToLocalDateTime(lastUsed));
            persistentLoginRepository.save(pl);
        });

    }

    @Override
    public @Nullable PersistentRememberMeToken getTokenForSeries(String seriesId) {
        return persistentLoginRepository.findById(seriesId).map(login -> new PersistentRememberMeToken(
                login.getUsername(),
                login.getSeries(),
                login.getToken(),
                converLocalDateTimeToDate(login.getLastUsed()))).orElse(null);
    }

    @Override
    public void removeUserTokens(String username) {
        persistentLoginRepository.deleteByUsername(username);
    }

    private LocalDateTime converDateToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime();
    }

    private Date converLocalDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.of("UTC")).toInstant());
    }
}
