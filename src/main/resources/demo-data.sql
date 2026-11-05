INSERT INTO enclosure (name, habitat_type, capacity)
VALUES
    ('Lion Rock',     'SAVANNA', 2),
    ('Zebra Plains',  'SAVANNA', 4),
    ('Deer Meadow',   'FOREST',  4),
    ('Bear Forest',   'FOREST',  2),
    ('Penguin Bay',   'AQUATIC', 5),
    ('Arctic Coast',  'ARCTIC',  2),
    ('Jungle Island', 'JUNGLE',  4),
    ('Desert House',  'DESERT',  4),
    ('Empty Forest',  'FOREST',  3);

INSERT INTO species (name, habitat_type, diet_type)
VALUES
    ('Lion',       'SAVANNA', 'CARNIVORE'),
    ('Zebra',      'SAVANNA', 'HERBIVORE'),
    ('Deer',       'FOREST',  'HERBIVORE'),
    ('Brown bear', 'FOREST',  'OMNIVORE'),
    ('Penguin',    'AQUATIC', 'CARNIVORE'),
    ('Polar bear', 'ARCTIC',  'CARNIVORE'),
    ('Chimpanzee', 'JUNGLE',  'OMNIVORE'),
    ('Parrot',     'JUNGLE',  'OMNIVORE'),
    ('Camel',      'DESERT',  'HERBIVORE'),
    ('Tortoise',   'DESERT',  'HERBIVORE');

INSERT INTO animal (
    name, species_id, enclosure_id, birth_date,
    weight, size, feeding_interval_hours, photo,
    health_status, deleted_status
)
SELECT
    d.name, s.species_id, e.enclosure_id, d.birth_date,
    d.weight, d.size, d.feeding_interval_hours, d.photo,
    'HEALTHY', 'PRESENT'
FROM (VALUES
          ('Leo', 'Lion', 'Lion Rock', DATE '2018-06-12',
           190, 120, 24, 'demo-lion.png'),

          ('Simba', 'Lion', 'Lion Rock', DATE '2020-04-19',
           175, 115, 24, 'demo-lion.png'),

          ('Marty', 'Zebra', 'Zebra Plains', DATE '2019-05-10',
           320, 140, 8, 'demo-zebra.png'),

          ('Zara', 'Zebra', 'Zebra Plains', DATE '2021-07-22',
           290, 135, 8, 'demo-zebra.png'),

          ('Bambi', 'Deer', 'Deer Meadow', DATE '2022-03-15',
           95, 105, 8, 'demo-deer.png'),

          ('Daisy', 'Deer', 'Deer Meadow', DATE '2021-05-08',
           110, 110, 8, 'demo-deer.png'),

          ('Baloo', 'Brown bear', 'Bear Forest', DATE '2016-02-20',
           280, 150, 12, 'demo-brown-bear.png'),

          ('Pingu', 'Penguin', 'Penguin Bay', DATE '2020-11-03',
           6.5, 75, 6, 'demo-penguin.png'),

          ('Pippa', 'Penguin', 'Penguin Bay', DATE '2021-10-14',
           5.8, 70, 6, 'demo-penguin.png'),

          ('Nanook', 'Polar bear', 'Arctic Coast', DATE '2017-12-05',
           450, 160, 24, 'demo-polar-bear.png'),

          ('Kiki', 'Chimpanzee', 'Jungle Island', DATE '2019-08-11',
           45, 90, 6, 'demo-chimpanzee.png'),

          ('Momo', 'Chimpanzee', 'Jungle Island', DATE '2020-09-17',
           40, 85, 6, 'demo-chimpanzee.png'),

          ('Rio', 'Parrot', 'Jungle Island', DATE '2022-06-01',
           1.1, 85, 8, 'demo-parrot.png'),

          ('Sahara', 'Camel', 'Desert House', DATE '2018-04-23',
           520, 200, 12, 'demo-camel.png'),

          ('Spike', 'Tortoise', 'Desert House', DATE '2010-07-09',
           35, 60, 24, 'demo-tortoise.png')
     ) AS d (
             name, species_name, enclosure_name, birth_date,
             weight, size, feeding_interval_hours, photo
    )
         JOIN species s ON s.name = d.species_name
         JOIN enclosure e ON e.name = d.enclosure_name;
