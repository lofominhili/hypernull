# HyperNull

HyperNull - восходящая звезда в мире криптовалют. Боты собирают hypernull-коины (монеты), генерируемые в адресном пространстве видеопамяти. Спрос на HyperNull высок, в майнинге большая конкуренция, на каждую монету претендует сразу несколько майнеров. А система безопасности видеопамяти блокирует некоторые ячейки адресного пространства, усложняя задачу.

Ваша задача разработать алгоритм бота, который будет эффективно майнить hypernull-коины: соберет как можно больше монет за фиксированное количество раундов. Бот перемещается по ячейкам двумерной карты. За один ход (или раунд) он может перейти на соседнюю ячейку по горизонтали, вертикали или диагонали, если она свободна. Левая нижняя ячейка карты задается координатами (0, 0). Карты замкнуты по ширине и высоте. Это означает, что если бот находится в крайней правой ячейке и перемещается на одну позицию вправо, он попадает в крайнюю левую ячейку (переходит через границу). Это правило применяется вдоль всех направлений и учитывается при вычислении расстояний между ячейками.

Монеты появляются на карте в случайных позициях. Каждая монета достается боту, который первым к ней приблизится. Кроме высокой конкуренции за монеты, процесс майнинга усложняется тем, что бот не знает полную схему карты: какие из ячеек заблокированы и где находятся все боты и монеты. Каждый бот "видит" карту только в пределах радиуса видимости вокруг текущей позиции.

☠️ *DEATHMATCH* ☠️

По умолчанию боты дружелюбны и соревнуются только в скорости и эффективности исследования карты. Но для желающих особенной наживы предусмотрен майнинг в режиме `DEATHMATCH`. В этом режиме боты могут нападать друг на друга и отнимать все собранные монеты.

## Майнинг

Майнинг проходит в виде матчей на сервере. Боты подключаются к серверу и регистрируют заявку на участи в матче. В заявке указывается название бота и режим матча: `FRIENDLY` или `DEATHMATCH`.

Сервер принимает заявку, готовит карту, собирает участников и запускает матч. На каждом раунде боты получают информацию об их текущем окружении и отправляют команды на перемещение по карте на сервер.

Матч длится в пределах ограничения по количеству раундов. Если бот проигрывает в схватке в режиме `DEATHMATCH`, он покидает матч до его завершения.

```
                +-----+                           +---------+
                | bot |                           | server  |
                +-----+                           +---------+
                   |                                   |
                   | чтение конфигурации из файла      |
                   |-----------------------------      |
                   |                            |      |
                   |<----------------------------      |
                   |                                   |
                   | подключение к server:port         |
                   |---------------------------------->|
                   |                                   |
                   |                             hello |
                   |<----------------------------------|
                   |                                   |
                   | register                          |
                   |---------------------------------->|
                   |                                   | ------------------------\
                   |                                   |-| инициализация матча   |
                   |                                   | | и ожидание участников |
                   |                                   | |-----------------------|
                   |                             match |
                   |<----------------------------------|
                   |                                   | ------------------------------------\
                   |                                   |-| обмен update/move в каждом раунде |
                   |                                   | |-----------------------------------|
                   |                            update |
                   |<----------------------------------|
-----------------\ |                                   |
| алгоритм бота  |-|                                   |
| работает здесь | |                                   |
|----------------| |                                   |
                   | move                              |
                   |---------------------------------->|
                   |                                   | -------------------------\
                   |                                   |-| матч для бота завершен |
                   |                                   | |------------------------|
                   |                        match_over |
                   |<----------------------------------|
                   |                                   |
```

- При запуске бот загружает [файл конфигурации](starter-bot/bot.properties), путь к которому задается первым аргументом командной строки и подключается к серверу.
- На каждое подключение сервер отправляет приветственное сообщение [`hello`](#hello).
- Бот отвечает на приветствие сообщением [`register`](#register), указывает желаемый режим матча и регистрационную информацию, подтверждая готовность участия в матче.
- Сервер регистрирует участника и инициализирует матч. При необходимости дожидается готовности других ботов.
- Когда состав участников матча сформирован, сервер отправляет всем ботам сообщение [`match`](#match) и запускает матч.
- На каждом раунде матча сервер отправляет сообщение [`update`](#update) c текущим состоянием всем активным ботам.
- Сервер ожидает команды [`move`](#move) и подтверждения хода от всех активных ботов и обновляет текущее состояние матча на основе полученных команд. Если бот не успевает прислать команду за отведенное время, он пропускает ход.
- Если бот выбывает из матча, сервер исключает его из списка активных и отправляет этому боту сообщение [`match_over`](#match_over).
- При достижении лимита по количеству раундов матч завершается. Всем активным ботам отправляется сообщение [`match_over`](#match_over).

## Протокол бота

Бот и сервер обмениваются сообщениями в текстовом формате.

```
command
param1 param1_value1 param1_value2 ... param1_valueN
param2 param2_value1 param2_value2 ... param2_valueN
...
paramN paramN_value1 paramN_value2 ... paramN_valueN
end
```

### hello

Отправляется сервером при подключении бота. В ответ бот отправляет на сервер сообщение [`register`](#register).

```
hello
end
```

### register

Отправляется ботом при подключении к серверу.

```
register
bot_name {BOT_NAME}
bot_secret {BOT_SECRET}
mode {MATCH_MODE}
end
```

- `BOT_NAME` название бота, по которому на сервере собирается статистика
- `BOT_SECRET` если `BOT_NAME` на сервере уже зарегистрирован, допуск к матчу возможен только при совпадении с указанным ранее `BOT_SECRET`
- `MATCH_MODE` режим матча: строка `FRIENDLY` (по умолчанию) или `DEATHMATCH`

### match

Отправляется сервером один раз при старте матча.

```
match
num_rounds {NUM_ROUNDS}
mode {MATCH_MODE}
map_width {MAP_WIDTH}
map_height {MAP_HEIGHT}
num_bots {NUM_BOTS}
your_id {YOUR_ID}
view_radius {VIEW_RADIUS}
mining_radius {MINING_RADIUS}
attack_radius {ATTACK_RADIUS}
move_time_limit {MOVE_TIME_LIMIT}
end
```

- `NUM_ROUNDS` количество раундов в матче
- `MATCH_MODE` строка `FRIENDLY` или `DEATHMATCH`
- `MAP_WIDTH` ширина карты [1, 32767]
- `MAP_HEIGHT` высота карты [1, 32767]
- `NUM_BOTS` количество ботов в матче [1, 64]
- `YOUR_ID` идентификатор/индекс текущего бота [0, `NUM_BOTS`)
- `VIEW_RADIUS` радиус видимости [1, 32767]
- `MINING_RADIUS` радиус майнинга монет <= `VIEW_RADIUS`
- `ATTACK_RADIUS` радиус атаки (для режима `DEATHMATCH`) <= `VIEW_RADIUS`
- `MOVE_TIME_LIMIT` временное ограничение на выполнение хода в миллисекундах >= 500

### update

Отправляется сервером в начале каждого раунда и содержит информацию о карте в пределах радиуса видимости бота. Для каждой непустой ячейки в сообщение включается параметр `bot` (бот, в том числе текущий игрок), `block` (препятствие) или `coin` (монета), определяющий ее координаты.

```
update
round {ROUND_NUMBER}
bot {X} {Y} {BOT_ID} {NUM_BOT_COINS}
block {X} {Y}
coin {X} {Y}
end
```

- `ROUND_NUMBER` номер текущего раунда, начиная с 1
- `NUM_BOT_COINS` количество монет, собранных ботом
- `BOT_ID` идентификатор бота

Сообщение всегда содержит информацию о текущем боте. Например, если в [`match`](#match) боту присвоен идентификатор 1

```
match
your_id 1
...
end
```

то следующий `update` означает, что у бота 24 монеты, он находится в ячейке (7, 106) и в зоне его видимости находится бот с идентификатором 0, у которого на одну монету больше

```
update
bot 14 99 0 25
bot 7 106 1 24
...
end
```

### move

Отправляется ботом для совершения хода в каждом раунде. За раунд может быть отправлен только один `move`. Следующая команда может быть отправлена в следующем раунде, о начале которого сервер сигнализирует сообщением [`update`](#update).

```
move
offset {DX} {DY}
end
```

- `DX` перемещение бота по X: -1, 0, 1
- `DY` перемещение бота по Y: -1, 0, 1

### match_over

Отправляется сервером при завершении матча для текущего бота.

```
match_over
end
```

---

🥷 *Банзай!*

---

## Механика

Детали реализации сервера.

### Инициализация матча

- Случайным образом выбирается или генерируется карта.
- Боты размещаются в случайные стартовые позиции.
- Первичный вес (количество собранных монет) участников устанавливается равным 0.
- Проходит первичная генерация монет на карте.

### Изменение позиций ботов

Позиции ботов изменяются в соответствии с указанными в рамках хода командами и ограничениями на дистанцию перемещения. Бот может перейти в целевую ячейку, если она свободна. Если ячейка заблокирована, бот остается в текущем положении. Когда в режиме `FRIENDLY` одну и ту же ячейку указывают в качестве целевой несколько ботов, они остаются в текущем положении.

### Атака (DEATHMATCH)

Если расстояние между ботами сокращается до `ATTACK_RADIUS`, они атакуют друг-друга. Среди всех атакующих выбирается бот с наибольшим количеством собранных монет. Боты в зоне его атаки считаются побежденными, все их монеты переходят атакующему. Побежденные боты выбывают из матча. Процесс повторяется для активных ботов, для которых по-прежнему выполняется условие атаки.

### Сбор монет

Если расстояние между позицией бота и монетой сокращается до `MINING_RADIUS`, бот получает +1 к весу (количеству собранных монет), а монета удаляется с карты. Если претендентов на монету несколько, она достается боту с большим весом или случайному боту, если вес всех претендентов совпадает.

### Генерация монет

У каждого матча есть два параметра: период генерации монет `COIN_SPAWN_PERIOD` и объем генерации за один раунд `COIN_SPAWN_VOLUME`. Период определяет раунды, на которых монеты появляются на карте. Если текущий номер раунда кратен `COIN_SPAWN_PERIOD`, в свободных ячейках карты появляется `COIN_SPAWN_VOLUME` новых монет. Позиции выбираются случайно, но по возможности распределяются симметрично относительно стартовых позиций участников. Перед стартом матча до первого хода происходит первичная генерация.

### Расстояние между ячейками

Так как карты замкнуты по ширине и высоте, расстояние вдоль оси "через границу" может оказаться меньше, чем расстояние "внутри" карты. На сервере используются следующие формулы для вычисления расстояний.

Для двух позиций `p1 = (x1, y1)` и `p2 = (x2, y2)`

`dx = min(|x1 - x2|, MAP_WIDTH - |x1 - x2|)`

`dy = min(|y1 - y2|, MAP_HEIGHT - |y1 - y2|)`

Квадрат расстояния между позициями

`d^2 = dx^2 + dy^2`

Считается, что ячейки находятся в пределах радиуса `R`, если

`d^2 <= R^2`

## Форматы

### Формат карты

TODO

### Формат лога матча

TODO
