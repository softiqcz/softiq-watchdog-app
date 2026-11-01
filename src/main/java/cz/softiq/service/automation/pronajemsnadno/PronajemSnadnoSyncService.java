package cz.softiq.service.automation.pronajemsnadno;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class PronajemSnadnoSyncService {
  private final PronajemSnadnoSyncComponent component;

  String setLinkedInSearch() {
    return component.setLinkedInSearch();
  }
}
