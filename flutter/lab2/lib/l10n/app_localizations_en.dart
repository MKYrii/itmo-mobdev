// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'Contacts';

  @override
  String get searchHint => 'Search by name';

  @override
  String get sortAsc => 'A → Z';

  @override
  String get sortDesc => 'Z → A';

  @override
  String foundCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count contacts found',
      one: '1 contact found',
      zero: 'No contacts found',
    );
    return '$_temp0';
  }

  @override
  String get noContacts => 'No contacts in the address book';

  @override
  String get noSearchResults => 'Nothing found';

  @override
  String get loading => 'Loading…';

  @override
  String get permissionDeniedTitle => 'Access denied';

  @override
  String get permissionDeniedBody =>
      'The app needs access to your contacts to show them.';

  @override
  String get permissionRequest => 'Grant access';

  @override
  String get permissionPermanentlyDeniedBody =>
      'Access was permanently denied. Please enable it in settings.';

  @override
  String get openSettings => 'Open settings';

  @override
  String get errorTitle => 'Failed to read contacts';

  @override
  String get retry => 'Retry';

  @override
  String get contactDetails => 'Contact details';

  @override
  String get noPhone => 'No phone';

  @override
  String get noEmail => 'No email';

  @override
  String get phonesSection => 'Phones';

  @override
  String get emailsSection => 'Emails';
}
