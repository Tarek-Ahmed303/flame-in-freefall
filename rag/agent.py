
import re


class ResearchAgent:
    """
    Flame in Freefall conversational research agent.

    Routes messages to:
    1. A lightweight conversation handler.
    2. The existing RAG + ML pipeline for combustion research.
    3. The configured LLM for general, out-of-domain questions.
    """

    def __init__(self, pipeline):
        self.pipeline = pipeline

    # ---------------------------------------------------------
    # 1. CLASSIFY THE QUESTION
    # ---------------------------------------------------------
    def classify_question(self, question: str) -> str:
        q = question.lower().strip()
        # Introduction / name detection
        if re.search(r"\bmy name is\s+[a-zA-Z]+", q):
            return "introduction"
            name = match.group(1).capitalize() if match else None
            message = (
                f"Nice to meet you, {name}! 😊 "
                "I'm your Flame in Freefall Research Assistant. "
                "What would you like to explore today?"
                if name
                else "Nice to meet you! 😊 "
                "I'm your Flame in Freefall Research Assistant."
                "What would you like to explore today?"
            )
            return self._result(message, decision="conversation")
            
        # Friendly social messages
        if any(phrase in q for phrase in [
            "good to see you",
            "good to meet you",
            "nice to see you",
            "nice to meet you",
            "good to work with you",
            "nice working with you",
            "happy to see you",
            "glad to see you",
            "how are you",
            "how are you doing",
            "i appreciate you",
            "you're helpful",
            "you are helpful",
        ]):
            return "friendly_conversation"
        # Greetings
        greetings = {
            "hi", "hello", "hey", "hi there",
            "hello there", "hey there", "howdy",
            "good morning", "good afternoon", "good evening",
        }
        if q.strip("!.,? ") in greetings:
            return "greeting"

        # Common conversational messages
        if q.strip("!.,? ") in {
            "thanks", "thank you", "thanks a lot",
            "thank you so much", "thx", "thanks","thank you",
            "thanks a lot","thank you so much","thx","good job",
            "well done","great job","awesome","nice","great",
            "excellent",
        }:
            return "thanks"

        if q.strip("!.,? ") in {
            "bye", "goodbye", "see you", "see you later",
        }:
            return "goodbye"

        if q.strip("!.,? ") in {
            "who are you", "what are you", "what is your name",
        }:
            return "identity"

        if re.search(
            r"\bmy name is\s+[A-Za-z][A-Za-z '\-]{0,49}",
            question,
            re.IGNORECASE,
        ):
            return "introduction"

        # Experiment-specific questions
        has_experiment_id = bool(
            re.search(r"\bEXP[-_ ]?\d+\b", question, re.IGNORECASE)
        )

        if has_experiment_id and any(
            word in q
            for word in (
                "compare", "comparison", "difference",
                "versus", " vs ", "compared",
            )
        ):
            return "experiment_comparison"

        if has_experiment_id and any(
            word in q
            for word in (
                "predict", "prediction", "risk",
                "probability", "probability of fire",
            )
        ):
            return "experiment_risk"

        # Domain-related terminology
        domain_keywords = [
            "flame",
            "combustion",
            "microgravity",
            "oxygen concentration",
            "oxygen level",
            "fire",
            "ignition",
            "soot",
            "heat release",
            "flame spread",
            "flame temperature",
            "ambient temperature",
            "pressure",
            "flow velocity",
            "flow configuration",
            "fuel",
            "burning",
            "extinguish",
            "extinction",
            "fire safety",
            "fire risk",
            "risk level",
            "nasa experiment",
            "nasa experiments",
            "exp-",
            "exp_",
            "exp ",
        ]

        if any(keyword in q for keyword in domain_keywords):
            if any(
                word in q
                for word in (
                    "predict", "prediction", "risk",
                    "probability", "estimate",
                )
            ):
                return "risk_analysis"

            if any(
                word in q
                for word in (
                    "compare", "comparison", "difference",
                    "versus", " vs ",
                )
            ):
                return "scientific_comparison"

            return "scientific_question"

        # Everything else gets a general LLM answer.
        return "general_question"

    # ---------------------------------------------------------
    # 2. MAIN ENTRY POINT
    # ---------------------------------------------------------
    def answer(self, question: str) -> dict:
        question = question.strip()

        if not question:
            return self._result(
                "Please enter a message or question so I can help.",
                decision="conversation",
            )
            
        task = self.classify_question(question)
        print("DEBUG TASK:", task)
        if task == "introduction":
            match = re.search(
                r"\bmy name is\s+([a-zA-Z]+)",
                question,
                re.IGNORECASE
            )
            name = match.group(1).capitalize() if match else None
            message = (
                f"Nice to meet you, {name}! 😊 "
                "I'm your Flame in Freefall Research Assistant. "
                "What would you like to explore today?"
            )
            return self._result(message, decision="conversation")

            
        # Ordinary conversation does not need retrieval.
        if task == "friendly_conversation":
            return self._result(
        "It's great to see you too! 😊 I'm happy to help. "
        "What would you like to explore today?",
        decision="conversation",
        )
    
        if task == "greeting":
            return self._result(
                "Hello! 👋 I'm the Flame in Freefall Research "
                "Assistant. You can chat with me or ask about "
                "NASA microgravity combustion experiments, flame "
                "behavior, and fire-risk predictions.",
                decision="conversation",
            )
            if task == "introduction":
                match = re.search(
                    r"\bmy name is\s+([A-Za-z]+)",
                    question,
                    re.IGNORECASE
                )
            name = match.group(1).strip().capitalize() if match else None
            
            if name:
                message = (
                    f"Nice to meet you, {name}! 😊 "
                    "I'm your Flame in Freefall Research Assistant. "
                    "What would you like to explore today?"
                )
            else:
                message = (
                    "Nice to meet you! 😊 "
                    "I'm your Flame in Freefall Research Assistant. "
                    "What would you like to explore today?"
                )
            return self._result(message, decision="conversation")

        if task == "thanks":
            return self._result(
                "You're welcome! 😊 I'm happy to help. "
                "Feel free to ask another question whenever you're ready.",
                decision="conversation",
            )

        if task == "goodbye":
            return self._result(
                "Goodbye! 👋 Thanks for visiting Flame in Freefall. "
                "Come back whenever you'd like to explore combustion "
                "research or fire safety.",
                decision="conversation",
            )

        if task == "identity":
            return self._result(
                "I'm the Flame in Freefall Research Assistant. "
                "I help explain NASA microgravity combustion research, "
                "retrieve experimental evidence, and work with the "
                "project's fire-risk prediction model.",
                decision="conversation",
            )

        # Combustion and experiment questions use the existing RAG + ML.
        if task in {
            "scientific_question",
            "scientific_comparison",
            "experiment_comparison",
            "experiment_risk",
            "risk_analysis",
        }:
            return self.pipeline.answer(question)

        # General questions use the configured LLM without pretending
        # that the answer came from the NASA knowledge base.
        # Out-of-domain questions get a domain-aware response.
        return self._off_topic_response(question)

    # ---------------------------------------------------------
    # 3. GENERAL LLM ANSWER
    # ---------------------------------------------------------
    
    def _off_topic_response(self, question: str) -> dict:
        message = (
        "I'm here to help with questions about fire behavior, "
        "combustion, NASA microgravity experiments, and fire safety. "
        "Let's explore something related to these topics! 😊"
        )
        return self._result(
        message,
        decision="out_of_domain",
        )


        try:
            # Reuse the LLM client already configured in the RAG pipeline.
            llm_client = self.pipeline.response_generator.llm_client
            response = llm_client.generate(prompt=prompt)

            if not isinstance(response, dict):
                raise ValueError("The LLM did not return a JSON object.")

            response.setdefault("answer", "I'm sorry, I couldn't formulate an answer.")
            response.setdefault("experimental_evidence", [])
            response.setdefault("prediction", None)
            response.setdefault("observed_trends", [])
            response.setdefault("interpretation", "")
            response.setdefault("safety_insight", "")
            response.setdefault("limitations", [])
            response.setdefault("sources", [])
            response["model_used"] = "general_llm"

            # General LLM responses must never expose fabricated
            # experimental evidence or predictions.
            response["experimental_evidence"] = []
            response["prediction"] = None
            response["observed_trends"] = []
            response["sources"] = []

            return {
                "response": response,
                "answerability": {
                    "answerable": True,
                    "decision": "general_llm",
                    "reason": (
                        "General question answered without claiming "
                        "NASA knowledge-base evidence."
                    ),
                },
                "retrieved_results": [],
            }

        except Exception as exc:
            print(f"[Research Agent] General LLM error: {exc}")

            return self._result(
                "I'm sorry, I couldn't generate a response right now. "
                "Please try again in a moment.",
                decision="general_llm",
            )

    # ---------------------------------------------------------
    # 4. CONSISTENT RESPONSE FORMAT
    # ---------------------------------------------------------
    @staticmethod
    def _result(message: str, decision: str) -> dict:
        return {
            "response": {
                "answer": message,
                "experimental_evidence": [],
                "prediction": None,
                "observed_trends": [],
                "interpretation": "",
                "safety_insight": "",
                "limitations": [],
                "sources": [],
                "model_used": "conversation_handler",
            },
            "answerability": {
                "answerable": True,
                "decision": decision,
                "reason": "Handled by the conversational assistant.",
            },
            "retrieved_results": [],
        }
