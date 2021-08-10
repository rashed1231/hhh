package org.unichain.core.db;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.unichain.core.capsule.FutureTokenPackCapsule;

import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static org.unichain.core.config.Parameter.DatabaseConstants.TOKEN_ISSUE_COUNT_LIMIT_MAX;

@Slf4j(topic = "DB")
@Component
public class FutureTokenPackStore extends UnichainStoreWithRevoking<FutureTokenPackCapsule> {

  @Autowired
  protected FutureTokenPackStore(@Value("token-pack") String dbName) {
    super(dbName);
  }

  @Override
  public FutureTokenPackCapsule get(byte[] key) {
    return super.getUnchecked(key);
  }

  public List<FutureTokenPackCapsule> getAllTokens() {
    return Streams.stream(iterator())
        .map(Entry::getValue)
        .collect(Collectors.toList());
  }

  private List<FutureTokenPackCapsule> getTokenPaginated(List<FutureTokenPackCapsule> tokenList, long offset, long limit) {
    if (limit < 0 || offset < 0) {
      return null;
    }

    if (tokenList.size() <= offset) {
      return null;
    }
    tokenList.sort(Comparator.comparing(index -> index.getInstance().getDealsCount()));
    limit = limit > TOKEN_ISSUE_COUNT_LIMIT_MAX ? TOKEN_ISSUE_COUNT_LIMIT_MAX : limit;
    long end = offset + limit;
    end = end > tokenList.size() ? tokenList.size() : end;
    return tokenList.subList((int) offset, (int) end);
  }

  public List<FutureTokenPackCapsule> getTokenPaginated(long offset, long limit) {
    return getTokenPaginated(getAllTokens(), offset, limit);
  }
}