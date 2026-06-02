Feature: Reembolsos de pagos aprobados
  Como administrador del comercio
  Quiero solicitar reembolsos sobre pagos aprobados
  Para gestionar devoluciones con trazabilidad completa

  Background:
    Given el sistema de pagos está listo para recibir peticiones
    And un comercio verificado con credenciales activas

  @HU016 @camino-feliz
  Scenario: Reembolso total exitoso de un pago aprobado
    Given existe una transacción en estado COMPLETED
    When se envía una solicitud POST a "/api/v1/transactions/{transactionId}/refund-full" con:
      """
      { "reason": "Cliente solicita devolución total" }
      """
    Then la respuesta debe tener código 200
    And el campo "status" debe ser "REFUNDED"

  @HU016 @error
  Scenario: No se puede reembolsar un pago no aprobado
    Given una transacción en estado "CREATED"
    When se envía una solicitud POST a "/api/v1/transactions/{transactionId}/refund-full" con:
      """
      { "reason": "Intento inválido" }
      """
    Then la respuesta debe tener código 409

  @HU017 @camino-feliz
  Scenario: Reembolso parcial exitoso dentro del monto disponible
    Given existe una transacción en estado COMPLETED
    When se envía una solicitud POST a "/api/v1/transactions/{transactionId}/refund-partial" con:
      """
      { "amount": 5000, "reason": "Devolución de artículo" }
      """
    Then la respuesta debe tener código 200
    And el campo "status" debe ser "COMPLETED"

  @HU017 @error
  Scenario: No es posible reembolsar más del monto disponible
    Given existe una transacción en estado COMPLETED
    When se envía una solicitud POST a "/api/v1/transactions/{transactionId}/refund-partial" con:
      """
      { "amount": 99999999, "reason": "Intento de exceder monto" }
      """
    Then la respuesta debe tener código 409