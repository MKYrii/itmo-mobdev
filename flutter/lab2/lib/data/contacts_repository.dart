import 'package:flutter_contacts/flutter_contacts.dart';
import 'package:permission_handler/permission_handler.dart' as ph;

import '../models/contact_data.dart';

/// Состояние загрузки контактов — то, что показывает UI
enum ContactsLoadState {
  initial,
  loading,
  ready,
  denied,
  permanentlyDenied,
  empty,
  error,
}

class ContactsRepository {
  ContactsLoadState state = ContactsLoadState.initial;
  List<ContactData> contacts = [];
  String? errorMessage;

  /// Проверяем разрешение и загружаем контакты
  Future<void> load() async {
    state = ContactsLoadState.loading;

    // Снова используем permission_handler для контроля разрешений
    ph.PermissionStatus status = await ph.Permission.contacts.status;
    if (status.isDenied) {
      status = await ph.Permission.contacts.request();
    }
    if (status.isPermanentlyDenied) {
      state = ContactsLoadState.permanentlyDenied;
      return;
    }
    if (!status.isGranted) {
      state = ContactsLoadState.denied;
      return;
    }

    try {
      final raw = await FlutterContacts.getAll(
        properties: {
          ContactProperty.name,
          ContactProperty.phone,
          ContactProperty.email,
        },
      );

      contacts = raw
          .map((c) => ContactData(
        id: c.id ?? '',
        displayName: (c.displayName?.isNotEmpty ?? false)
            ? c.displayName!
            : '—',
        phones: c.phones.map((p) => p.number).toList(),
        emails: c.emails.map((e) => e.address).toList(),
      ))
          .toList();

      state = contacts.isEmpty
          ? ContactsLoadState.empty
          : ContactsLoadState.ready;
    } catch (e) {
      errorMessage = e.toString();
      state = ContactsLoadState.error;
    }
  }

  /// Поиск по уже загруженному списку, без обращения к системе
  List<ContactData> search(String query, {bool ascending = true}) {
    final q = query.trim().toLowerCase();
    final result = q.isEmpty
        ? List<ContactData>.from(contacts)
        : contacts
        .where((c) => c.displayName.toLowerCase().contains(q))
        .toList();

    result.sort(
          (a, b) => ascending
          ? a.displayName.toLowerCase().compareTo(b.displayName.toLowerCase())
          : b.displayName.toLowerCase().compareTo(a.displayName.toLowerCase()),
    );

    return result;
  }
}