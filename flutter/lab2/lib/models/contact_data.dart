class ContactData {
  final String id;
  final String displayName;
  final List<String> phones;
  final List<String> emails;

  const ContactData({
    required this.id,
    required this.displayName,
    required this.phones,
    required this.emails,
  });

  bool get hasEmail => emails.isNotEmpty;

  bool get hasPhone => phones.isNotEmpty;
}
