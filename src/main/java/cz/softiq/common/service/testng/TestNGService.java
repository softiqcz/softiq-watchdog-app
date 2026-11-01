package cz.softiq.common.service.testng;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class TestNGService {
  private final TestNGComponent component;

  void runTestNg(String path) {
    component.runTestNg(path);
  }

  List<String> executeWithExceptionHandling(
      Supplier<String> operation, List<String> exceptions, List<String> successMessages) {
    return component.executeWithExceptionHandling(operation, exceptions, successMessages);
  }

  <T> List<String> executeWithExceptionHandling(
      T input,
      Function<T, String> operation,
      List<String> exceptions,
      List<String> successMessages) {
    return component.executeWithExceptionHandling(input, operation, exceptions, successMessages);
  }

  <T, U> List<String> executeWithExceptionHandling(
      T input1,
      U input2,
      BiFunction<T, U, String> operation,
      List<String> exceptions,
      List<String> successMessages) {
    return component.executeWithExceptionHandling(
        input1, input2, operation, exceptions, successMessages);
  }
}
