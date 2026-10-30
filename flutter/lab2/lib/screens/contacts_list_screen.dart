import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import '../data/contacts_repository.dart';
import '../models/contact_data.dart';
import '../l10n/app_localizations.dart';
import 'contact_details_screen.dart';

import 'package:permission_handler/permission_handler.dart' as ph;

class ContactsListScreen extends StatefulWidget {
  const ContactsListScreen({super.key});

  @override
  State<ContactsListScreen> createState() => _ContactsListScreenState();
}

class _ContactsListScreenState extends State<ContactsListScreen>
    with WidgetsBindingObserver {
  final _repo = ContactsRepository();
  final _searchController = TextEditingController();

  String _query = '';
  bool _ascending = true;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _load();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _searchController.dispose();
    super.dispose();
  }

  /// Возврат из настроек — перечитываем состояние разрешения.
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _load();
    }
  }

  Future<void> _load() async {
    setState(() {});
    await _repo.load();
    if (mounted) setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.appTitle)),
      body: SafeArea(child: _buildBody(l10n)),
    );
  }

  Widget _buildBody(AppLocalizations l10n) {
    switch (_repo.state) {
      case ContactsLoadState.initial:
      case ContactsLoadState.loading:
        return Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const CircularProgressIndicator(),
              const SizedBox(height: 12),
              Text(l10n.loading),
            ],
          ),
        );

      case ContactsLoadState.denied:
        return _messageWithButton(
          title: l10n.permissionDeniedTitle,
          body: l10n.permissionDeniedBody,
          buttonText: l10n.permissionRequest,
          onPressed: _load,
        );

      case ContactsLoadState.permanentlyDenied:
        return _messageWithButton(
          title: l10n.permissionDeniedTitle,
          body: l10n.permissionPermanentlyDeniedBody,
          buttonText: l10n.openSettings,
            onPressed: () async {
              await ph.openAppSettings();
            },
        );

      case ContactsLoadState.empty:
        return Center(child: Text(l10n.noContacts));

      case ContactsLoadState.error:
        return _messageWithButton(
          title: l10n.errorTitle,
          body: _repo.errorMessage ?? '',
          buttonText: l10n.retry,
          onPressed: _load,
        );

      case ContactsLoadState.ready:
        return _buildList(l10n);
    }
  }

  Widget _buildList(AppLocalizations l10n) {
    final filtered = _repo.search(_query, ascending: _ascending);

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(8),
          child: TextField(
            controller: _searchController,
            decoration: InputDecoration(
              prefixIcon: const Icon(Icons.search),
              hintText: l10n.searchHint,
              border: const OutlineInputBorder(),
            ),
            onChanged: (value) => setState(() => _query = value),
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(l10n.foundCount(filtered.length)),
              TextButton.icon(
                onPressed: () => setState(() => _ascending = !_ascending),
                icon: Icon(
                  _ascending ? Icons.arrow_upward : Icons.arrow_downward,
                ),
                label: Text(_ascending ? l10n.sortAsc : l10n.sortDesc),
              ),
            ],
          ),
        ),
        Expanded(
          child: filtered.isEmpty
              ? Center(child: Text(l10n.noSearchResults))
              : ListView.builder(
                  itemCount: filtered.length,
                  itemBuilder: (context, i) {
                    final c = filtered[i];
                    return ListTile(
                      leading: CircleAvatar(
                        child: Text(
                          c.displayName.characters.first.toUpperCase(),
                        ),
                      ),
                      title: Text(c.displayName),
                      subtitle: Text(
                        c.hasPhone ? c.phones.first : l10n.noPhone,
                      ),
                      trailing: c.hasEmail
                          ? const Icon(Icons.email_outlined)
                          : null,
                      onTap: () {
                        Navigator.of(context).push(
                          MaterialPageRoute(
                            builder: (_) => ContactDetailsScreen(contact: c),
                          ),
                        );
                      },
                    );
                  },
                ),
        ),
      ],
    );
  }

  Widget _messageWithButton({
    required String title,
    required String body,
    required String buttonText,
    required VoidCallback onPressed,
  }) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              title,
              style: Theme.of(context).textTheme.titleLarge,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 12),
            Text(body, textAlign: TextAlign.center),
            const SizedBox(height: 20),
            FilledButton(onPressed: onPressed, child: Text(buttonText)),
          ],
        ),
      ),
    );
  }
}
