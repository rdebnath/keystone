import 'package:dio/dio.dart';
import 'package:flutter/material.dart';

/// The RFC 9457 `detail` of a failed API call, or [fallback] when the response carries none.
///
/// The decoded error body is read once, here, and never leaves this function
/// (`docs/CODING_GUIDELINES_FRONTEND.md` §14): a `403`, `409` or `400` from the backend explains itself
/// ("Cannot delete tenant with users") and that explanation is what the user should see.
String apiErrorMessage(Object error, String fallback) {
  if (error is DioException) {
    final data = error.response?.data;
    if (data is Map<String, dynamic>) {
      final detail = data['detail'];
      if (detail is String && detail.isNotEmpty) {
        return detail;
      }
    }
  }
  return fallback;
}

/// Reports a failed API call in a snack bar, preferring the backend's own explanation.
void showApiError(BuildContext context, Object error, String fallback) {
  ScaffoldMessenger.of(
    context,
  ).showSnackBar(SnackBar(content: Text(apiErrorMessage(error, fallback))));
}

/// Reports a successful mutation in a snack bar.
void showApiSuccess(BuildContext context, String message) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
}
