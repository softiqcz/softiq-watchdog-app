package cz.softiq.tests.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

@SpringBootTest(classes = cz.softiq.watchdog.WatchdogApplication.class)
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class})
public class MainWatchdogTestClass extends MainBrowserTestClass {}
