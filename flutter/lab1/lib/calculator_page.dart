import 'package:flutter/material.dart';

import 'calculator.dart';

class CalculatorPage extends StatefulWidget {
  const CalculatorPage({super.key});

  @override
  State<CalculatorPage> createState() => _CalculatorPageState();
}

class _CalculatorPageState extends State<CalculatorPage> with RestorationMixin {
  final _expression = RestorableString('');
  final _history = RestorableStringN(null);
  final _memory = RestorableDouble(0);
  final _lastResult = RestorableStringN(null);
  final _error = RestorableStringN(null);

  static const _navy = Color(0xFF102A43);
  static const _blue = Color(0xFF1F4E79);

  @override
  String? get restorationId => 'calculator_page';

  @override
  void restoreState(RestorationBucket? oldBucket, bool initialRestore) {
    registerForRestoration(_expression, 'expression');
    registerForRestoration(_history, 'history');
    registerForRestoration(_memory, 'memory');
    registerForRestoration(_lastResult, 'last_result');
    registerForRestoration(_error, 'error');
  }

  List<Calculation> get _items {
    final saved = _history.value;
    if (saved == null || saved.isEmpty) return [];
    return saved
        .split('\n')
        .where((line) => line.contains('\t'))
        .map((line) {
          final parts = line.split('\t');
          return Calculation(parts.first, parts.skip(1).join('\t'));
        })
        .toList();
  }

  void _saveHistory(List<Calculation> items) {
    _history.value = items
        .map((item) => '${item.expression}\t${item.result}')
        .join('\n');
  }

  void _press(String key) {
    setState(() {
      _error.value = null;
      switch (key) {
        case 'M':
        case 'MR':
          _expression.value += Calculator.format(_memory.value);
          break;
        case 'M−':
        case 'M-':
          _changeMemory(-1);
          break;
        case 'MC':
          _memory.value = 0;
          break;
        case 'C':
          _expression.value = '';
          _lastResult.value = null;
          break;
        case '⌫':
          if (_expression.value.isNotEmpty) {
            _expression.value =
                _expression.value.substring(0, _expression.value.length - 1);
          }
          break;
        case '=':
          _calculate();
          break;
        case '±':
          _changeSign();
          break;
        case '%':
          _percent();
          break;
        case 'M+':
          _changeMemory(1);
          break;
        default:
          _expression.value += key;
      }
    });
  }

  void _calculate() {
    try {
      final original = _expression.value;
      final result = Calculator.format(Calculator.evaluate(original));
      final items = _items;
      items.insert(0, Calculation(original, result));
      _saveHistory(items.take(10).toList());
      _lastResult.value = result;
      _expression.value = result;
    } on FormatException catch (exception) {
      _error.value = exception.message.toString();
    }
  }

  void _changeSign() {
    final expression = _expression.value;
    final match = RegExp(r'(-?\d*\.?\d+)$').firstMatch(expression);
    if (match == null) {
      if (expression.isEmpty || expression.endsWith('(')) {
        _expression.value += '-';
      }
      return;
    }
    final number = match.group(0)!;
    final changed = number.startsWith('-') ? number.substring(1) : '-$number';
    _expression.value = expression.replaceRange(match.start, match.end, changed);
  }

  void _percent() {
    final expression = _expression.value;
    final match = RegExp(r'(\d*\.?\d+)$').firstMatch(expression);
    if (match == null) return;
    final value = double.tryParse(match.group(0)!);
    if (value == null) return;
    _expression.value = expression.replaceRange(
      match.start,
      match.end,
      Calculator.format(value / 100),
    );
  }

  void _changeMemory(double sign) {
    try {
      _memory.value += sign * Calculator.evaluate(_expression.value);
    } on FormatException catch (exception) {
      _error.value = exception.message.toString();
    }
  }

  void _selectHistory(Calculation item) {
    setState(() {
      _expression.value = item.expression;
      _error.value = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    final landscape = MediaQuery.orientationOf(context) == Orientation.landscape;
    return Scaffold(
      appBar: AppBar(
        toolbarHeight: 28,
        title: const Text('Калькулятор', style: TextStyle(fontSize: 16)),
      ),
      body: SafeArea(
        child: landscape ? _buildLandscape() : _buildPortrait(),
      ),
    );
  }

  Widget _buildPortrait() {
    const keys = [
      ['C', '⌫', '(', ')', '÷'],
      ['7', '8', '9', '%', '×'],
      ['4', '5', '6', '±', '-'],
      ['1', '2', '3', 'M+', '+'],
      ['0', '.', '00', '=', ''],
    ];
    return Column(
      children: [
        Expanded(flex: 3, child: _buildDisplay()),
        SizedBox(height: 125, child: _buildHistory()),
        Expanded(flex: 6, child: _buildKeypad(keys)),
      ],
    );
  }

  Widget _buildLandscape() {
    const keys = [
      ['C', '⌫', '(', ')', '÷'],
      ['7', '8', '9', '%', '×'],
      ['4', '5', '6', '±', '-'],
      ['1', '2', '3', 'M+', '+'],
      ['0', '.', '00', '=', ''],
    ];
    return Row(
      children: [
        Expanded(
          flex: 4,
          child: Column(
            children: [
              Expanded(flex: 3, child: _buildDisplay()),
              Expanded(flex: 2, child: _buildHistory()),
            ],
          ),
        ),
        Expanded(flex: 5, child: _buildKeypad(keys)),
      ],
    );
  }

  Widget _buildDisplay() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 6, 20, 6),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              _memoryKey('M'),
              _memoryKey('M+'),
              _memoryKey('M−'),
              _memoryKey('MC'),
            ],
          ),
          Align(
            alignment: Alignment.centerRight,
            child: Padding(
              padding: const EdgeInsets.only(top: 2),
              child: Text(
                'Память: ${Calculator.format(_memory.value)}',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(color: Colors.blueGrey, fontSize: 12),
              ),
            ),
          ),
          const Spacer(),
          if (_lastResult.value != null)
            Text('Ответ: ${_lastResult.value}',
                textAlign: TextAlign.right,
                style: const TextStyle(color: Colors.blueGrey, fontSize: 14)),
          SingleChildScrollView(
            reverse: true,
            scrollDirection: Axis.horizontal,
            child: Text(
              _expression.value.isEmpty ? '0' : _expression.value,
              maxLines: 1,
              style: const TextStyle(
                color: _navy,
                fontSize: 34,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          if (_error.value != null)
            Text(_error.value!,
                textAlign: TextAlign.right,
                style: const TextStyle(color: Colors.red, fontSize: 12)),
        ],
      ),
    );
  }

  Widget _buildKeypad(List<List<String>> keys) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(8, 4, 8, 8),
      child: Column(
        children: keys
            .map((row) => Expanded(
                  child: Row(
                    children: row
                        .map((key) => Expanded(
                              child: key.isEmpty
                                  ? const SizedBox.shrink()
                                  : _buildKey(key),
                            ))
                        .toList(),
                  ),
                ))
            .toList(),
      ),
    );
  }

  Widget _memoryKey(String key) {
    return Padding(
      padding: const EdgeInsets.only(left: 4),
      child: SizedBox(
        height: 30,
        child: OutlinedButton(
          style: OutlinedButton.styleFrom(
            foregroundColor: _navy,
            padding: const EdgeInsets.symmetric(horizontal: 8),
            visualDensity: VisualDensity.compact,
            side: const BorderSide(color: Color(0xFFDCE6F0)),
          ),
          onPressed: () => _press(key),
          child: Text(key),
        ),
      ),
    );
  }

  Widget _buildHistory() {
    final items = _items;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
          child: Text('История',
              style: TextStyle(color: _navy, fontWeight: FontWeight.w600)),
        ),
        Expanded(
          child: items.isEmpty
              ? const Center(
                  child: Text('Вычислений пока нет',
                      style: TextStyle(color: Colors.blueGrey)),
                )
              : ListView.builder(
                  itemCount: items.length,
                  itemBuilder: (context, index) {
                    final item = items[index];
                    return ListTile(
                      dense: true,
                      title: Text(item.expression, maxLines: 1, overflow: TextOverflow.ellipsis),
                      trailing: Text('= ${item.result}',
                          style: const TextStyle(color: _blue)),
                      onTap: () => _selectHistory(item),
                    );
                  },
                ),
        ),
      ],
    );
  }

  Widget _buildKey(String key) {
    final isAction = const {'C', '⌫', '(', ')', '÷', '%', '×', '±', '-', '+', 'M+'}
        .contains(key);
    final isEquals = key == '=';
    return Padding(
      padding: const EdgeInsets.all(4),
      child: FilledButton(
        style: FilledButton.styleFrom(
          backgroundColor: isEquals ? _navy : (isAction ? const Color(0xFFDCE6F0) : Colors.white),
          foregroundColor: isEquals ? Colors.white : _navy,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          padding: EdgeInsets.zero,
        ),
        onPressed: () => _press(key),
        child: Text(key,
            style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w500)),
      ),
    );
  }
}
