(comment

  (def res-idx
    (->> (:Entity (:resources (world/context)))
         (filterv (fn [[rt v]]
                    (and (= "resource" (:type v))
                         (= (:module v) "fhir-4.0.0"))))
         (reduce (fn [idx [rt v]]
                   (assoc idx rt (select-keys v [:description])))
                 {})))

  res-idx

  (def grp
    {:Convormance #{:CapabilityStatement
                    :StructureDefinition
                    :ImplementationGuide
                    :SearchParameter
                    :MessageDefinition
                    :OperationDefinition
                    :CompartmentDefinition
                    :StructureMap
                    :GraphDefinition
                    :ExampleScenario}

     :Terminology #{:CodeSystem
                   :ValueSet
                   :ConceptMap
                   :NamingSystem
                   :TerminologyCapabilities}

     :Security #{:Provenance
                :AuditEvent
                :Consent}

     :Documents #{:Composition
                 :DocumentManifest
                 :DocumentReference
                 :CatalogEntry}
     :Individuals #{:Patient
                   :Practitioner
                   :PractitionerRole
                   :RelatedPerson
                   :Person
                   :Group}
     :Management #{:Encounter
                  :EpisodeOfCare
                  :Flag
                  :List
                  :Library}
     :Workflow #{:Task
                :Appointment
                :AppointmentResponse
                :Schedule
                :Slot
                 :VerificationResult}
     :Medications #{:MedicationRequest
                  :MedicationAdministration
                  :MedicationDispense
                  :MedicationStatement
                  :Medication
                  :MedicationKnowledge
                  :Immunization
                  :ImmunizationEvaluation
                    :ImmunizationRecommendation}
     :Diagnostics #{:Observation
                  :Media
                  :DiagnosticReport
                  :Specimen
                  :BodyStructure
                  :ImagingStudy
                  :QuestionnaireResponse
                    :MolecularSequence}
     :Finantital #{
                 :Coverage
                 :CoverageEligibilityRequest
                 :CoverageEligibilityResponse
                 :EnrollmentRequest
                 :EnrollmentResponse
                 :Claim
                 :ClaimResponse
                 :Invoice
                 :PaymentNotice
                 :PaymentReconciliation
                 :Account
                 :ChargeItem
                 :ChargeItemDefinition
                 :Contract
                 :ExplanationOfBenefit
                 :InsurancePlan
                  
                  }
     })

  (def grp-idx
    (->> grp
         (map-indexed (fn [i [k v]]
                        [i k v]
                        ))
         (reduce (fn [acc [i k rts]]
                   (->> rts
                        (reduce (fn [acc rt]
                                  (assoc acc rt (inc i))
                                  ) acc))) {})))

  ;; grp-idx


  (def attrs
    (->> (get-in (world/context) [:resources :Attribute])
         ;; (take 100)
         vals
         (reduce (fn [idx el]
                   (let [rt (keyword (get-in el [:resource :id]))]
                     (if (and (= "Reference" (get-in el [:type :id]))
                              (get idx rt))
                       (update-in idx [rt :els] conj {:path (:path el) :refers (:refers el)})
                       idx)))
                 res-idx)))

  (def graph 
    (->> attrs
         (reduce
          (fn [acc [rt {els :els}]]
            (let [acc (update acc :nodes conj {:id (name rt)
                                               :group (get grp-idx rt 0)})]
              (reduce (fn [acc el]
                        (->> (:refers el)
                             (reduce (fn [acc target]
                                       (-> acc
                                           (update :links conj {:source (name rt)
                                                                    :target target
                                                                    :label (str/join "." (mapv name (:path el)))
                                                                :value 1})
                                           (update :nodes
                                                   (fn [ns]
                                                     (->> ns
                                                          (mapv
                                                           (fn [n]
                                                             (if (= target (:id n))
                                                               (update n :incom (fn [x] (inc (or x 0))))
                                                               n)))))
                                                   ))) acc))
                        ) acc els))

            ) {:nodes [{:id "Resource"}] :links []})))

  (spit "/Users/niquola/visual-fhir/data.json"
        (cheshire.core/generate-string graph {:pretty true}))

  (first attrs)




  )
