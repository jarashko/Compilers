# Compilers — micro language

Учебный компилятор для небольшого языка **micro**: лексер, парсер, AST, семантический анализ, оптимизации на AST, tree-walking интерпретатор и собственное промежуточное представление **LLMR**.

Проект по структуре близок к [compilLab](https://github.com/Ran4er/compilLab), но вместо LLVM IR используется **LLMR** (Low-Level Micro Representation).

---

## Содержание

- [Архитектура](#архитектура)
- [Структура проекта](#структура-проекта)
- [Синтаксис языка](#синтаксис-языка)
- [Как запустить](#как-запустить)
- [LLMR](#llmr)
- [Оптимизации](#оптимизации)
- [Сравнение с compilLab](#сравнение-с-compillab)

---

## Архитектура

```
Исходный код (.txt)
        │
        ▼
  ┌─────────────┐
  │   Lexer     │  токены
  └─────────────┘
        │
        ▼
  ┌─────────────┐
  │   Parser    │  AST (Recursive Descent)
  └─────────────┘
        │
        ▼
  ┌─────────────┐
  │ Semantic    │  типы, функции, области видимости
  │ Analyzer    │  (SymbolTable)
  └─────────────┘
        │
        ▼
  ┌─────────────────────────┐
  │ ConstantFoldingOptimizer │  свёртка констант + DCE
  └─────────────────────────┘
        │
        ├──────────────────────────────┐
        ▼                              ▼
  ┌─────────────┐               ┌──────────────┐
  │ Interpreter │               │ AstToLlmr    │
  │ eval/execute│               │ Translator   │
  │ + Runtime   │               └──────────────┘
  │ Environment │                      │
  └─────────────┘                      ▼
                                 LlmrOptimizer
                                       │
                                       ▼
                                 LlmrInterpreter
```

---

## Структура проекта

```
src/main/java/micro/
├── Main.java
├── Lexer.java, Parser.java, Token.java, TokenType.java
├── ast/                    # AST + AstVisitor
├── interp/
│   ├── SemanticAnalyzer.java
│   ├── SymbolTable.java
│   ├── Interpreter.java    # eval() + execute()
│   ├── RuntimeEnvironment.java
│   ├── FunctionInfo.java
│   ├── ReturnException.java
│   └── Value.java, ValueType.java, ArrayValue.java
├── opt/
│   ├── ConstantFoldingOptimizer.java
│   ├── DeadCodeEliminator.java
│   └── ConstantEvaluator.java
└── llmr/                   # LLMR IR + бэкенд
    ├── AstToLlmrTranslator.java
    ├── LlmrInterpreter.java
    └── LlmrOptimizer.java
```

---

## Синтаксис языка

### Типы

| Тип | Пример |
|-----|--------|
| `number` | `42`, `3.14` |
| `bool` | `true`, `false` |
| `string` | `"hello"` |
| `array` | `[1, 2, 3]` |
| `void` | только возврат функции |

### Переменные

```
var x = 10;
var msg = "hi";
var nums = [1, 2, 3];
```

### Массивы

```
print nums[0];
nums[1] = 99;
print nums;
```

### Функции

```
function number add(number a, number b) {
    return a + b;
}

function number factorial(number n) {
    if (n <= 1) {
        return 1;
    }
    return n * factorial(n - 1);
}

print add(3, 4);
print factorial(6);
```

### Управление

```
if (x > 0) { print x; } else { print 0; }
while (n > 0) { n = n - 1; }
print expr;
```

---

## Как запустить

### Требования

- Java 17+
- Maven 3+

### Сборка и запуск

```bash
mvn compile
mvn exec:java
mvn exec:java -Dexec.args=test_functions.txt
mvn exec:java -Dexec.args=test_arrays.txt
```

### Пример вывода

```
============================================================
  Built-in demo
============================================================

[1] Lexical Analysis...
    Tokens: ...
[2] Parsing...
    AST built.
[3] Type Checking...
    Type check passed.
[4] Optimizing (Constant Folding + Dead Code Elimination)...
    Optimization done.
[5] Interpreting optimized AST...
    -- Output ------------------------------
14
14
720
1
[1, 42, 3]
    ----------------------------------------
[6] Generating LLMR...
    Written to: demo_outputs/output.llmr
```

---

## LLMR

**LLMR** — стековое промежуточное представление, отделяющее фронтенд от бэкенда.

Примеры инструкций: `LOAD_CONST`, `LOAD_VAR`, `STORE_VAR`, `ADD`, `JUMP_IF_FALSE`, `MAKE_ARRAY`, `ARRAY_LOAD`, `HALT`.

Программы с **функциями** выполняются через AST-интерпретатор; LLMR для них пока не генерируется.

---

## Оптимизации

### Constant Folding

| Выражение | Результат |
|-----------|-----------|
| `2 + 2` | `4` |
| `3 * 4 + 2` | `14` |
| `x * 0` | `0` |
| `x * 1` | `x` |
| `x + 0` | `x` |

### Dead Code Elimination

```
if (false) { ... }   → удаляется
while (false) { }    → удаляется
var unused = 1;      → удаляется, если переменная не читается
```

---

## Runtime

- **`RuntimeEnvironment`** — стек областей видимости (`pushScope` / `popScope`)
- **`Interpreter.eval(expr)`** — вычисление выражений
- **`Interpreter.execute(stmt)`** — выполнение операторов
- **`ReturnException`** — control-flow для `return`

---

## Сравнение с compilLab

| | compilLab | Этот проект |
|---|-----------|-------------|
| IR | LLVM | LLMR |
| Нативный код | ✅ | ❌ |
| Функции | ✅ | ✅ |
| Массивы | ❌ | ✅ |
| Строки | ❌ | ✅ |
| RuntimeEnvironment | ✅ | ✅ |

---

## Автор

Репозиторий: [github.com/jarashko/Compilers](https://github.com/jarashko/Compilers)
