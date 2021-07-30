package org.unichain.core.db;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.unichain.core.capsule.CreateTokenCapsule;

import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static org.unichain.core.config.Parameter.DatabaseConstants.TOKEN_ISSUE_COUNT_LIMIT_MAX;

@Slf4j(topic = "DB")
@Component
public class TokenStore extends UnichainStoreWithRevoking<CreateTokenCapsule> {

  @Autowired
  protected TokenStore(@Value("token-issue") String dbName) {
    super(dbName);
  }

  @Override
  public CreateTokenCapsule get(byte[] key) {
    return super.getUnchecked(key);
  }

  public List<CreateTokenCapsule> getAllTokens() {
    return Streams.stream(iterator())
        .map(Entry::getValue)
        .collect(Collectors.toList());
  }

  private List<CreateTokenCapsule> getTokenPaginated(List<CreateTokenCapsule> tokenList, long offset, long limit) {
    if (limit < 0 || offset < 0) {
      return null;
    }

    if (tokenList.size() <= offset) {
      return null;
    }
    tokenList.sort(Comparator.comparing(o -> o.getName().toStringUtf8()));
    limit = limit > TOKEN_ISSUE_COUNT_LIMIT_MAX ? TOKEN_ISSUE_COUNT_LIMIT_MAX : limit;
    long end = offset + limit;
    end = end > tokenList.size() ? tokenList.size() : end;
    return tokenList.subList((int) offset, (int) end);
  }

  public List<CreateTokenCapsule> getTokenPaginated(long offset, long limit) {
    return getTokenPaginated(getAllTokens(), offset, limit);
  }
}
