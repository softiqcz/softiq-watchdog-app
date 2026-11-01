package cz.softiq.common.service.testng;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ITestNGService extends IBeanProvider, IBaseService {
  private TestNGService getTestNGService() {
    return getBean(TestNGService.class);
  }

  default void runTestNg(String suitePath) {
    getTestNGService().runTestNg(suitePath);
  }

  default List<String> executeWithExceptionHandling(
      Supplier<String> action, List<String> exceptions, List<String> successes) {
    return getTestNGService().executeWithExceptionHandling(action, exceptions, successes);
  }

  default <T> List<String> executeWithExceptionHandling(
      T input, Function<T, String> action, List<String> exceptions, List<String> successes) {
    return getTestNGService().executeWithExceptionHandling(input, action, exceptions, successes);
  }

  default <T, U> List<String> executeWithExceptionHandling(
      T first,
      U second,
      BiFunction<T, U, String> action,
      List<String> exceptions,
      List<String> successes) {
    return getTestNGService()
        .executeWithExceptionHandling(first, second, action, exceptions, successes);
  }
}
