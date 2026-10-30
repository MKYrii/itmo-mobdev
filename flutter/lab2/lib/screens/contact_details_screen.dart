import 'package:flutter/material.dart';

import '../models/contact_data.dart';
import '../l10n/app_localizations.dart';

class ContactDetailsScreen extends StatelessWidget {
  final ContactData contact;

  const ContactDetailsScreen({super.key, required this.contact});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.contactDetails)),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Center(
              child: CircleAvatar(
                radius: 40,
                child: Text(
                  contact.displayName.characters.first.toUpperCase(),
                  style: const TextStyle(fontSize: 32),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Center(
              child: Text(
                contact.displayName,
                style: Theme.of(context).textTheme.headlineSmall,
                textAlign: TextAlign.center,
              ),
            ),
            const SizedBox(height: 24),

            Text(
              l10n.phonesSection,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            if (contact.phones.isEmpty)
              Text(l10n.noPhone)
            else
              ...contact.phones.map(
                (p) =>
                    ListTile(leading: const Icon(Icons.phone), title: Text(p)),
              ),

            const SizedBox(height: 16),
            Text(
              l10n.emailsSection,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            if (contact.emails.isEmpty)
              Text(l10n.noEmail)
            else
              ...contact.emails.map(
                (e) =>
                    ListTile(leading: const Icon(Icons.email), title: Text(e)),
              ),
          ],
        ),
      ),
    );
  }
}
