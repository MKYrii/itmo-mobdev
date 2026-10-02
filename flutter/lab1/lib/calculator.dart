class Calculation {
  const Calculation(this.expression, this.result);

  final String expression;
  final String result;
}

class Calculator {
  static double evaluate(String expression) {
    final parser = _ExpressionParser(expression);
    final result = parser.parse();
    if (!result.isFinite) throw const FormatException('Некорректный результат');
    return result;
  }

  static String format(double value) {
    final result = value == value.roundToDouble()
        ? value.toInt().toString()
        : value.toString();
    if (result.length <= 10) return result;
    return value.toStringAsExponential(4);
  }
}

class _ExpressionParser {
  _ExpressionParser(String expression)
      : _tokens = _tokenize(expression);

  final List<String> _tokens;
  int _position = 0;

  double parse() {
    if (_tokens.isEmpty) throw const FormatException('Введите выражение');
    final value = _sum();
    if (_position != _tokens.length) {
      throw const FormatException('Проверьте выражение');
    }
    return value;
  }

  double _sum() {
    var value = _product();
    while (_match('+') || _match('-')) {
      final operation = _tokens[_position - 1];
      final next = _product();
      value = operation == '+' ? value + next : value - next;
    }
    return value;
  }

  double _product() {
    var value = _unary();
    while (_match('×') || _match('÷')) {
      final operation = _tokens[_position - 1];
      final next = _unary();
      if (operation == '÷' && next == 0) {
        throw const FormatException('Деление на ноль');
      }
      value = operation == '×' ? value * next : value / next;
    }
    return value;
  }

  double _unary() {
    if (_match('+')) return _unary();
    if (_match('-')) return -_unary();
    return _primary();
  }

  double _primary() {
    if (_match('(')) {
      final value = _sum();
      if (!_match(')')) throw const FormatException('Не закрыта скобка');
      return value;
    }

    if (_position >= _tokens.length) {
      throw const FormatException('Проверьте выражение');
    }
    final token = _tokens[_position++];
    final number = double.tryParse(token);
    if (number == null) throw const FormatException('Проверьте выражение');
    return number;
  }

  bool _match(String token) {
    if (_position >= _tokens.length || _tokens[_position] != token) {
      return false;
    }
    _position++;
    return true;
  }

  static List<String> _tokenize(String expression) {
    final tokens = <String>[];
    var index = 0;
    while (index < expression.length) {
      final char = expression[index];
      if (char.trim().isEmpty) {
        index++;
      } else if ('()+-×÷'.contains(char)) {
        tokens.add(char);
        index++;
      } else if (char == '.' || (char.codeUnitAt(0) >= 48 && char.codeUnitAt(0) <= 57)) {
        final start = index;
        var dots = 0;
        while (index < expression.length) {
          final current = expression[index];
          final isDigit = current.codeUnitAt(0) >= 48 && current.codeUnitAt(0) <= 57;
          if (isDigit) {
            index++;
          } else if (current == '.') {
            dots++;
            if (dots > 1) throw const FormatException('Несколько точек в числе');
            index++;
          } else {
            break;
          }
        }
        final number = expression.substring(start, index);
        if (number == '.') throw const FormatException('Проверьте число');
        tokens.add(number);
      } else {
        throw const FormatException('Проверьте выражение');
      }
    }
    return tokens;
  }
}
