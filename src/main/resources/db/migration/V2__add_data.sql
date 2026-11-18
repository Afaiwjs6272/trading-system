INSERT INTO instruments (seccode, type, lot_size, min_step) VALUES
                                                                ('GAZP',  'STOCK',    1,  0.01),
                                                                ('SBER',  'STOCK',    1,  0.01),
                                                                ('USD/RUB', 'CURRENCY', 1000, 0.0001)
    ON CONFLICT (seccode) DO NOTHING;