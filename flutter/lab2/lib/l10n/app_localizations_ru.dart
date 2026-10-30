// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Russian (`ru`).
class AppLocalizationsRu extends AppLocalizations {
  AppLocalizationsRu([String locale = 'ru']) : super(locale);

  @override
  String get appTitle => 'Контакты';

  @override
  String get searchHint => 'Поиск по имени';

  @override
  String get sortAsc => 'А → Я';

  @override
  String get sortDesc => 'Я → А';

  @override
  String foundCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'Найдено контактов: $count',
      one: 'Найден 1 контакт',
      zero: 'Ничего не найдено',
    );
    return '$_temp0';
  }

  @override
  String get noContacts => 'В адресной книге нет контактов';

  @override
  String get noSearchResults => 'Ничего не найдено';

  @override
  String get loading => 'Загрузка…';

  @override
  String get permissionDeniedTitle => 'Доступ запрещён';

  @override
  String get permissionDeniedBody =>
      'Приложению нужен доступ к контактам, чтобы их показать.';

  @override
  String get permissionRequest => 'Разрешить доступ';

  @override
  String get permissionPermanentlyDeniedBody =>
      'Доступ запрещён навсегда. Включите его в настройках.';

  @override
  String get openSettings => 'Открыть настройки';

  @override
  String get errorTitle => 'Не удалось прочитать контакты';

  @override
  String get retry => 'Повторить';

  @override
  String get contactDetails => 'Информация о контакте';

  @override
  String get noPhone => 'Нет телефона';

  @override
  String get noEmail => 'Нет email';

  @override
  String get phonesSection => 'Телефоны';

  @override
  String get emailsSection => 'Email';
}
